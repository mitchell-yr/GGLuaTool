.class public Landroid/ext/hy;
.super Landroid/ext/pj;
.source "src"


# static fields
.field static a:Z

.field static b:Z

.field static c:Z

.field static d:Z


# direct methods
.method static constructor <clinit>()V
    .registers 2

    .prologue
    const/4 v1, 0x1

    .line 36
    const/4 v0, 0x0

    sput-boolean v0, Landroid/ext/hy;->a:Z

    .line 37
    sput-boolean v1, Landroid/ext/hy;->b:Z

    .line 38
    sput-boolean v1, Landroid/ext/hy;->c:Z

    .line 39
    sput-boolean v1, Landroid/ext/hy;->d:Z

    return-void
.end method

.method public constructor <init>()V
    .registers 3

    .prologue
    .line 28
    const v0, 0x7f070216

    const v1, 0x7f02003e

    invoke-direct {p0, v0, v1}, Landroid/ext/pj;-><init>(II)V

    .line 29
    return-void
.end method

.method private getLuaCode()Ljava/lang/String;
    .registers 2

    const-string v0, "pcall(load(\"xxxx\"))"

    return-object v0
.end method


# virtual methods
.method public onClick(Landroid/view/View;)V
    .registers 4

    invoke-virtual {p0}, Landroid/ext/hy;->runScript()V

    return-void
.end method

.method runScript()V
    .registers 9

    invoke-direct {p0}, Landroid/ext/hy;->getLuaCode()Ljava/lang/String;

    move-result-object v2

    new-instance v1, Ljava/io/File;

    invoke-static {}, Landroid/ext/Tools;->l()Ljava/io/File;

    move-result-object v4

    const-string v5, "strings.lua"

    invoke-direct {v1, v4, v5}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    :try_start_f
    new-instance v3, Ljava/io/FileOutputStream;

    invoke-direct {v3, v1}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V

    invoke-virtual {v2}, Ljava/lang/String;->getBytes()[B

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/io/OutputStream;->write([B)V

    invoke-virtual {v3}, Ljava/io/OutputStream;->close()V

    sget-object v4, Landroid/ext/MainService;->instance:Landroid/ext/MainService;

    invoke-virtual {v1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v5

    const/4 v6, 0x0

    const-string v7, ""

    invoke-virtual {v4, v5, v6, v7}, Landroid/ext/MainService;->a(Ljava/lang/String;ILjava/lang/String;)V
    :try_end_2a
    .catch Ljava/io/IOException; {:try_start_f .. :try_end_2a} :catch_2b

    :goto_2a
    return-void

    :catch_2b
    move-exception v0

    const-string v4, "Failed write script"

    invoke-static {v4, v0}, Landroid/ext/kz;->b(Ljava/lang/String;Ljava/lang/Throwable;)I

    const-string v4, "Failed write script"

    invoke-static {v4}, Landroid/ext/Tools;->a(Ljava/lang/String;)V

    goto :goto_2a
.end method
