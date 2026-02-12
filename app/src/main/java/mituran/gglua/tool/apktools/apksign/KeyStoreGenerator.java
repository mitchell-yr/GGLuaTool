package mituran.gglua.tool.apktools.apksign;

import android.util.Log;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * 密钥库生成器
 * 使用SpongyCastle (BouncyCastle for Android)
 */
public class KeyStoreGenerator {
    private static final String TAG = "KeyStoreGenerator";
    private static final String KEYSTORE_TYPE = "BKS";
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    static {
        // 注册SpongyCastle Provider
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 生成密钥库
     *
     * @param keystorePath     密钥库保存路径
     * @param keystorePassword 密钥库密码
     * @param keyAlias         密钥别名
     * @param keyPassword      密钥密码
     * @param dnName           证书DN名称，如 "CN=Android Debug,O=Android,C=US"
     * @param validityYears    证书有效期（年）
     * @return 是否成功
     */
    public boolean generateKeyStore(String keystorePath, String keystorePassword,
                                    String keyAlias, String keyPassword,
                                    String dnName, int validityYears) {
        FileOutputStream fos = null;
        try {
            Log.d(TAG, "开始生成密钥库...");
            Log.d(TAG, "密钥库路径: " + keystorePath);
            Log.d(TAG, "证书DN: " + dnName);

            // 1. 生成密钥对
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            Log.d(TAG, "密钥对已生成 (RSA " + KEY_SIZE + " bits)");

            // 2. 生成自签名证书
            X509Certificate certificate = generateCertificate(
                    dnName,
                    keyPair,
                    validityYears
            );

            Log.d(TAG, "证书已生成");
            Log.d(TAG, "证书主题: " + certificate.getSubjectDN().getName());
            Log.d(TAG, "证书有效期: " + certificate.getNotBefore() + " 至 " + certificate.getNotAfter());

            // 3. 创建密钥库
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, "SC");
            keyStore.load(null, keystorePassword.toCharArray());

            // 4. 将密钥和证书放入密钥库
            keyStore.setKeyEntry(
                    keyAlias,
                    keyPair.getPrivate(),
                    keyPassword.toCharArray(),
                    new Certificate[]{certificate}
            );

            Log.d(TAG, "密钥已添加到密钥库");

            // 5. 保存密钥库到文件
            fos = new FileOutputStream(keystorePath);
            keyStore.store(fos, keystorePassword.toCharArray());

            Log.d(TAG, "密钥库已保存: " + keystorePath);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "生成密钥库失败", e);
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 使用SpongyCastle生成X509自签名证书
     */
    private X509Certificate generateCertificate(String dnName, KeyPair keyPair, int validityYears)
            throws Exception {

        // 计算证书有效期
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + (validityYears * 365L * 24 * 60 * 60 * 1000));

        // 生成序列号
        BigInteger serialNumber = BigInteger.valueOf(now);

        // 创建X500Name（证书主题和颁发者）
        X500Name issuer = new X500Name(dnName);
        X500Name subject = new X500Name(dnName);

        // 获取公钥信息
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(
                keyPair.getPublic().getEncoded()
        );

        // 创建证书构建器
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                issuer,
                serialNumber,
                startDate,
                endDate,
                subject,
                publicKeyInfo
        );

        // 创建签名器
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .setProvider("SC")  // SpongyCastle provider
                .build(keyPair.getPrivate());

        // 生成证书
        X509CertificateHolder certHolder = certBuilder.build(signer);

        // 转换为X509Certificate
        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider("SC")
                .getCertificate(certHolder);

        Log.d(TAG, "使用SpongyCastle生成证书成功");

        return certificate;
    }

    /**
     * 验证密钥库是否有效
     */
    public static boolean validateKeyStore(String keystorePath, String keystorePassword,
                                           String keyAlias, String keyPassword) {
        try {
            Log.d(TAG, "验证密钥库: " + keystorePath);

            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, "SC");
            java.io.FileInputStream fis = new java.io.FileInputStream(keystorePath);
            keyStore.load(fis, keystorePassword.toCharArray());
            fis.close();

            // 验证别名是否存在
            if (!keyStore.containsAlias(keyAlias)) {
                Log.e(TAG, "密钥库中不存在别名: " + keyAlias);
                return false;
            }

            // 尝试获取密钥
            KeyStore.ProtectionParameter protParam =
                    new KeyStore.PasswordProtection(keyPassword.toCharArray());
            KeyStore.PrivateKeyEntry pkEntry =
                    (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyAlias, protParam);

            if (pkEntry == null) {
                Log.e(TAG, "无法获取密钥");
                return false;
            }

            // 验证证书链
            Certificate[] certChain = pkEntry.getCertificateChain();
            if (certChain == null || certChain.length == 0) {
                Log.e(TAG, "证书链为空");
                return false;
            }

            Log.d(TAG, "密钥库验证成功");
            Log.d(TAG, "证书链长度: " + certChain.length);

            // 打印证书信息
            if (certChain[0] instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) certChain[0];
                Log.d(TAG, "证书主题: " + cert.getSubjectDN().getName());
                Log.d(TAG, "证书有效期: " + cert.getNotBefore() + " 至 " + cert.getNotAfter());
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "密钥库验证失败", e);
            e.printStackTrace();
            return false;
        }
    }
}