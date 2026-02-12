package mituran.gglua.tool.apktools.apksign;

import android.util.Log;

import com.android.apksig.ApkSigner;
import com.android.apksig.apk.ApkFormatException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mituran.gglua.tool.apktools.apksign.SignatureConfig;

/**
 * APK签名包装器
 * 使用Google官方apksig库进行APK签名
 */
public class ApkSignerWrapper {
    private static final String TAG = "ApkSignerWrapper";

    /**
     * 使用配置签名APK
     *
     * @param inputApk  输入APK文件
     * @param outputApk 输出APK文件
     * @param config    签名配置
     * @return 是否成功
     */
    public static boolean signApk(File inputApk, File outputApk, SignatureConfig.Config config) {
        if (config == null) {
            Log.e(TAG, "签名配置为空");
            return false;
        }

        if (!SignatureConfig.validateConfig(config)) {
            Log.e(TAG, "签名配置无效");
            return false;
        }

        return signApk(
                inputApk,
                outputApk,
                config.keystorePath,
                config.keystorePassword,
                config.keyAlias,
                config.keyPassword
        );
    }

    /**
     * 使用密钥库签名APK
     *
     * @param inputApk         输入APK文件
     * @param outputApk        输出APK文件
     * @param keystorePath     密钥库路径
     * @param keystorePassword 密钥库密码
     * @param keyAlias         密钥别名
     * @param keyPassword      密钥密码
     * @return 是否成功
     */
    public static boolean signApk(File inputApk, File outputApk,
                                  String keystorePath, String keystorePassword,
                                  String keyAlias, String keyPassword) {
        FileInputStream fis = null;
        try {
            Log.d(TAG, "开始签名APK...");
            Log.d(TAG, "输入: " + inputApk.getAbsolutePath());
            Log.d(TAG, "输出: " + outputApk.getAbsolutePath());
            Log.d(TAG, "密钥库: " + keystorePath);

            // 1. 加载密钥库
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            fis = new FileInputStream(keystorePath);
            keyStore.load(fis, keystorePassword.toCharArray());

            Log.d(TAG, "密钥库已加载");

            // 2. 获取私钥和证书链
            KeyStore.ProtectionParameter protParam =
                    new KeyStore.PasswordProtection(keyPassword.toCharArray());
            KeyStore.PrivateKeyEntry pkEntry =
                    (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyAlias, protParam);

            if (pkEntry == null) {
                Log.e(TAG, "无法从密钥库获取密钥");
                return false;
            }

            PrivateKey privateKey = pkEntry.getPrivateKey();
            java.security.cert.Certificate[] certChain = pkEntry.getCertificateChain();

            if (certChain == null || certChain.length == 0) {
                Log.e(TAG, "证书链为空");
                return false;
            }

            // 转换为X509Certificate列表
            List<X509Certificate> certificates = new ArrayList<>();
            for (java.security.cert.Certificate cert : certChain) {
                if (cert instanceof X509Certificate) {
                    certificates.add((X509Certificate) cert);
                }
            }

            Log.d(TAG, "已获取私钥和证书链 (共 " + certificates.size() + " 个证书)");

            // 3. 配置ApkSigner
            ApkSigner.SignerConfig signerConfig = new ApkSigner.SignerConfig.Builder(
                    "CERT",  // 签名者名称
                    privateKey,
                    certificates
            ).build();

            // 4. 创建ApkSigner
            ApkSigner.Builder signerBuilder = new ApkSigner.Builder(
                    Collections.singletonList(signerConfig)
            )
                    .setInputApk(inputApk)
                    .setOutputApk(outputApk)
                    .setV1SigningEnabled(true)   // 启用V1签名（JAR签名）
                    .setV2SigningEnabled(true)   // 启用V2签名
                    .setV3SigningEnabled(true)   // 启用V3签名
                    .setV4SigningEnabled(false); // V4签名通常不需要

            // 设置最小SDK版本（如果需要）
            // signerBuilder.setMinSdkVersion(21);

            ApkSigner signer = signerBuilder.build();

            // 5. 执行签名
            Log.d(TAG, "开始执行签名...");
            signer.sign();

            Log.d(TAG, "APK签名完成: " + outputApk.getAbsolutePath());
            return true;

        } catch (ApkFormatException e) {
            Log.e(TAG, "APK格式错误", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "签名失败", e);
            e.printStackTrace();
            return false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 使用测试密钥签名APK（兼容旧方法）
     */
    public static boolean signApkWithTestKey(File inputApk, File outputApk) {
        Log.d(TAG, "使用默认测试密钥签名APK");
        return signApk(
                inputApk,
                outputApk,
                SignatureConfig.DEFAULT_KEYSTORE_PATH,
                SignatureConfig.DEFAULT_KEYSTORE_PASSWORD,
                SignatureConfig.DEFAULT_KEY_ALIAS,
                SignatureConfig.DEFAULT_KEY_PASSWORD
        );
    }

    /**
     * 验证APK签名
     */
    public static boolean verifyApk(File apkFile) {
        try {
            Log.d(TAG, "验证APK签名: " + apkFile.getAbsolutePath());

            com.android.apksig.ApkVerifier verifier =
                    new com.android.apksig.ApkVerifier.Builder(apkFile).build();

            com.android.apksig.ApkVerifier.Result result = verifier.verify();

            boolean isVerified = result.isVerified();
            Log.d(TAG, "签名验证结果: " + (isVerified ? "通过" : "失败"));

            if (!isVerified) {
                // 打印错误信息
                for (com.android.apksig.ApkVerificationIssue error : result.getErrors()) {
                    Log.e(TAG, "验证错误: " + error);
                }
            }

            return isVerified;

        } catch (Exception e) {
            Log.e(TAG, "验证APK签名失败", e);
            return false;
        }
    }

    /**
     * 获取APK签名信息
     */
    public static class SignatureInfo {
        public boolean isSigned;
        public boolean isV1Signed;
        public boolean isV2Signed;
        public boolean isV3Signed;
        public List<String> signerNames = new ArrayList<>();
        public String errorMessage;
    }

    /**
     * 获取APK的签名信息
     */
    public static SignatureInfo getSignatureInfo(File apkFile) {
        SignatureInfo info = new SignatureInfo();

        try {
            com.android.apksig.ApkVerifier verifier =
                    new com.android.apksig.ApkVerifier.Builder(apkFile).build();

            com.android.apksig.ApkVerifier.Result result = verifier.verify();

            info.isSigned = result.isVerified();
            info.isV1Signed = result.isVerifiedUsingV1Scheme();
            info.isV2Signed = result.isVerifiedUsingV2Scheme();
            info.isV3Signed = result.isVerifiedUsingV3Scheme();

            // 获取签名者信息
            for (com.android.apksig.ApkVerifier.Result.V1SchemeSignerInfo signerInfo :
                    result.getV1SchemeSigners()) {
                info.signerNames.add(signerInfo.getName());
            }

            if (!info.isSigned && !result.getErrors().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (com.android.apksig.ApkVerificationIssue error : result.getErrors()) {
                    sb.append(error.toString()).append("\n");
                }
                info.errorMessage = sb.toString();
            }

        } catch (Exception e) {
            Log.e(TAG, "获取签名信息失败", e);
            info.isSigned = false;
            info.errorMessage = e.getMessage();
        }

        return info;
    }
}