#include <stdio.h>
#include <conio.h>
#include <unistd.h>
#include <stdlib.h>
#include <thread>
#include <fcntl.h>
#include <dirent.h>
#include <pthread.h>
#include <math.h>
#include <iostream>
#include <string.h>
#include <stdio.h>
#include <assert.h>

struct MAPS
{
	long int addr;
	long int taddr;
	char* value;
	struct MAPS *next;
};
struct RESULT
{
	long int addr;
	struct RESULT *next;
};
struct FREEZE
{
	long int addr;				// 地址
	char *value;				// 值
	int type;					// 类型
	struct FREEZE *next;		// 指向下一节点的指针
};
#define LEN sizeof(struct MAPS)
#define FRE sizeof(struct FREEZE)
typedef struct MAPS *PMAPS;		// 存储maps的链表
typedef struct RESULT *PRES;	// 存储结果的链表
typedef struct FREEZE *PFREEZE;	// 存储冻结的数据的链表
typedef int TYPE;
typedef int RANGE;
typedef int COUNT;
typedef int COLOR;
typedef long int OFFSET;
typedef long int ADDRESS;
typedef char PACKAGENAME;
typedef unsigned char     uint8;
typedef unsigned long    uint32;
enum type{ DWORD, FLOAT, BYTE};
enum Range
{
	ALL,						// 所有内存
	B_BAD,						// B内存
	C_ALLOC,					// Ca内存
	C_BSS,						// Cb内存
	C_DATA,						// Cd内存
	C_HEAP,						// Ch内存
	JAVA_HEAP,					// Jh内存
	A_ANONMYOUS,				// A内存
	CODE_SYSTEM,				// Xs内存
	STACK,						// S内存
	ASHMEM						// As内存
};
enum Color
{
	COLOR_SILVERY,				// 银色
	COLOR_RED,					// 红色
	COLOR_GREEN,				// 绿色
	COLOR_YELLOW,				// 黄色
	COLOR_DARK_BLUE,			// 蓝色
	COLOR_PINK,					// 粉色
	COLOR_SKY_BLUE,				// 天蓝
	COLOR_WHITE					// 白色
};
static uint8 alphabet_map[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
static uint8 reverse_map[] =
{
     255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
     255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
     255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 62, 255, 255, 255, 63,
     52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 255, 255, 255, 255, 255, 255,
     255,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
     15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 255, 255, 255, 255, 255,
     255, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
     41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 255, 255, 255, 255, 255
};
PMAPS Res = NULL;				// 全局buff(保存数据的地方)
PFREEZE Pfreeze = NULL;			// 用于存储冻结的数据
PFREEZE pEnd = NULL;
PFREEZE pNew = NULL;
int FreezeCount = 0;			// 冻结数据个数
int Freeze = 0;					// 开关
int ttt = 0;
pthread_t pth;
pthread_t FrT;
char Fbm[64];					// 包名
long int delay = 30000;			// 冻结延迟,默认30000us
int ResCount = 0;				// 结果数量
int MemorySearchRange = 0;		// 0为所有
int SetTextColor(int color);// 设置文本颜色
int getRoot(char **argv);		// 获取root权限
int getPID(char bm[64]);		// 获取pid
void SetTar(char* pName); 		//设置全局目标进程 必须最先设置，不然此插件不生效
int SetSearchRange(int type);	// 设置搜索范围
PMAPS readmaps(int type);		// 读取maps文件
PMAPS readmaps_all();			// 读取maps文件
PMAPS readmaps_bad();			// 读取maps文件
PMAPS readmaps_c_alloc();		// 读取maps文件
PMAPS readmaps_c_bss();			// 读取maps文件
PMAPS readmaps_c_data();		// 读取maps文件
PMAPS readmaps_c_heap();		// 读取maps文件
PMAPS readmaps_java_heap();		// 读取maps文件
PMAPS readmaps_a_anonmyous();	// 读取maps文件
PMAPS readmaps_code_system();	// 读取maps文件
PMAPS readmaps_stack();			// 读取maps文件
PMAPS readmaps_ashmem();		// 读取maps文件

void BaseAddressSearch(char *value, int *gs, int type, long int BaseAddr);	// 基址搜索
PMAPS BaseAddressSearch_DWORD(int value, int *gs, long int BaseAddr, PMAPS pMap);	// DWORD
PMAPS BaseAddressSearch_FLOAT(float value, int *gs, long int BaseAddr, PMAPS pMap);	// FLOAT

void RangeMemorySearch(char *from_value, char *to_value, int *gs, int type);	// 范围搜索
PMAPS RangeMemorySearch_DWORD(int from_value, int to_value, int *gs, PMAPS pMap);	// DWORD
PMAPS RangeMemorySearch_FLOAT(float from_value, float to_value, int *gs, PMAPS pMap);	// FLOAT

void MemorySearch(char *value, int *gs, int TYPE);	// 类型搜索,这里value需要传入一个地址
PMAPS MemorySearch_DWORD(int value, int *gs, PMAPS pMap);	// 内存搜索DWORD
PMAPS MemorySearch_FLOAT(float value, int *gs, PMAPS pMap);	// 内存搜索FLOAT

void MemoryOffset(char *value, long int offset, int *gs, int type);	// 搜索偏移
PMAPS MemoryOffset_DWORD(int value, long int offset, PMAPS pBuff, int *gs);	// 搜索偏移DWORD
PMAPS MemoryOffset_FLOAT(float value, long int offset, PMAPS pBuff, int *gs);	// 搜索偏移FLOAT

void RangeMemoryOffset(char *from_value, char *to_value, long int offset, int *gs, int type);	// 范围偏移
PMAPS RangeMemoryOffset_DWORD(int from_value, int to_value, long int offset, PMAPS pBuff, int *gs);	// 搜索偏移DWORD
PMAPS RangeMemoryOffset_FLOAT(float from_value, float to_value, long int offset, PMAPS pBuff, int *gs);	// 搜索偏移FLOAT

void MemoryWrite(char *value, long int offset, int type);	// 内存写入
int MemoryWrite_DWORD(int value, PMAPS pBuff, long int offset);	// 内存写入DWORD
int MemoryWrite_FLOAT(float value, PMAPS pBuff, long int offset);	// 内存写入FLOAT

// 请使用独立线程
void MemoryWrite_Fr(char* value, OFFSET offset, TYPE type); //写入并冻结
int MemoryWrite_DWORD_Fr(int value, PMAPS pBuff, OFFSET offset); //写入整数并冻结
int MemoryWrite_FLOAT_Fr(float value, PMAPS pBuff, OFFSET offset); //写入单浮点并冻结


void GetAddress(long int addr, TYPE type); //读内存
void ReadAddress(long int addr, TYPE type); // 读内存
int GetAddress_DWORD(ADDRESS addr); //读整数
float GetAddress_FLOAT(ADDRESS addr); //读单浮点
char GetAddress_BYTE(ADDRESS addr);
PMAPS MemorySearch_BYTE(int value, COUNT * gs, PMAPS pMap);
PMAPS MemoryOffset_BYTE(int value, OFFSET offset, PMAPS pBuff, COUNT * gs);

void *SearchAddress(long int addr);	// 搜索地址中的值,返回一个指针
int WriteAddress(long int addr, void *value, int type);	// 修改地址中的值
void BypassGameSafe();			// 绕过游戏保护
void ReGameSafe(char *bm);		// 恢复游戏保护
void Print();					// 打印Res里面的内容
void ClearResults();			// 清除链表,释放空间
void ClearMaps(PMAPS pMap);		// 清空maps
int isapkinstalled(char *bm);	// 检测应用是否安装
int isapkrunning(char *bm);		// 检测应用是否运行
int killprocess(char *bm);		// 杀掉进程
char GetProcessState(char *bm);	// 获取进程状态
int killGG();					// 杀掉GG修改器
int killXs();					// 杀XS
int uninstallapk(char *bm);		// 静默卸载软件
int installapk(char *lj);		// 静默安装软件
int rebootsystem();				// 重启系统(手机)
int PutDate();					// 输出系统日期
int GetDate(char *date);		// 获取系统时间
PMAPS GetResults();				// 获取结果,返回头指针
int AddFreezeItem_All(char *Value, int type, long int offset);	// 冻结所有结果
int AddFreezeItem(long int addr, char *value, int type, long int offset);	// 增加冻结数据
int AddFreezeItem_DWORD(long int addr, char *value);	// DWORD
int AddFreezeItem_FLOAT(long int addr, char *value);	// FLOAT
int RemoveFreezeItem(long int addr);	// 清除固定冻结数据
int RemoveFreezeItem_All();		// 清空所有冻结数据
int StartFreeze();				// 开始冻结
int StopFreeze();				// 停止冻结
int SetFreezeDelay(long int De);// 设置冻结延迟
int PrintFreezeItems();			// 打印冻结表

int iPID;						// 读写目标PID
char* iPackage;					// 读写目标包名


int SetTextColor(COLOR color)
{
	switch (color)
	{
	case COLOR_SILVERY:printf("\033[30;1m"); break;
	case COLOR_RED:printf("\033[31;1m"); break;
	case COLOR_GREEN:printf("\033[32;1m"); break;
	case COLOR_YELLOW:printf("\033[33;1m"); break;
	case COLOR_DARK_BLUE:printf("\033[34;1m"); break;
	case COLOR_PINK:printf("\033[35;1m"); break;
	case COLOR_SKY_BLUE:printf("\033[36;1m"); break;
	case COLOR_WHITE:printf("\033[37;1m"); break;
	default:printf("\033[37;1m"); break;
	}
	return 0;
}

int getRoot(char **argv)
{
	char ml[64];
	sprintf(ml, "chmod 777 ", argv);
	//sprintf(ml, "su -c %s", argv);
	system(ml);
	return 0;
}

int getPID(PACKAGENAME * PackageName)
{
	DIR *dir = NULL;
	struct dirent *ptr = NULL;
	FILE *fp = NULL;
	char filepath[1024];
	char filetext[128];
	dir = opendir("/proc");
	if (NULL != dir)
	{
		while ((ptr = readdir(dir)) != NULL)
		{
			if ((strcmp(ptr->d_name, ".") == 0) || (strcmp(ptr->d_name, "..") == 0)) continue;
			if (ptr->d_type != DT_DIR) continue;
			sprintf(filepath, "/proc/%s/cmdline", ptr->d_name);
			fp = fopen(filepath, "r");
			if (NULL != fp)
			{
				fgets(filetext, sizeof(filetext), fp);
				if (strcmp(filetext, PackageName) == 0){ break;}
				fclose(fp);
			}
		}
	}
	if (readdir(dir) == NULL){ return 0;}
	closedir(dir);
	return atoi(ptr->d_name);
}

int SetSearchRange(TYPE type)
{
	switch (type)
	{
	case ALL:MemorySearchRange = 0; break;
	case B_BAD:MemorySearchRange = 1; break;
	case C_ALLOC:MemorySearchRange = 2; break;
	case C_BSS:MemorySearchRange = 3; break;
	case C_DATA:MemorySearchRange = 4; break;
	case C_HEAP:MemorySearchRange = 5; break;
	case JAVA_HEAP:MemorySearchRange = 6; break;
	case A_ANONMYOUS:MemorySearchRange = 7; break;
	case CODE_SYSTEM:MemorySearchRange = 8; break;
	case STACK:MemorySearchRange = 9; break;
	case ASHMEM:MemorySearchRange = 10; break;
	default:printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
	return 0;
}

PMAPS readmaps(TYPE type)
{
	PMAPS pMap = NULL;
	switch (type)
	{
	case ALL:pMap = readmaps_all(); break;
	case B_BAD:pMap = readmaps_bad(); break;
	case C_ALLOC:pMap = readmaps_c_alloc(); break;
	case C_BSS:pMap = readmaps_c_bss(); break;
	case C_DATA:pMap = readmaps_c_data(); break;
	case C_HEAP:pMap = readmaps_c_heap(); break;
	case JAVA_HEAP:pMap = readmaps_java_heap(); break;
	case A_ANONMYOUS:pMap = readmaps_a_anonmyous(); break;
	case CODE_SYSTEM:pMap = readmaps_code_system(); break;
	case STACK:pMap = readmaps_stack(); break;
	case ASHMEM:pMap = readmaps_ashmem(); break;
	default: printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
	if (pMap == NULL){ return 0;}
	return pMap;
}

PMAPS readmaps_all()
{
	PMAPS pHead = NULL;
	PMAPS pNew;
	PMAPS pEnd;
	pEnd = pNew = (PMAPS) malloc(LEN);
	FILE *fp;
	int i = 0, flag = 1;
	char lj[64], buff[256];
	sprintf(lj, "/proc/%d/maps", iPID);
	fp = fopen(lj, "r");
	if (fp == NULL)
	{
		puts("内存分析错误");
		return NULL;
	}
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取一行
		if (strstr(buff, "rw") != NULL && !feof(fp))
		{
			sscanf(buff, "%lx-%lx", &pNew->addr, &pNew->taddr);
			// 这里使用lx是为了能成功读取特别长的地址
			flag = 1;
		}
		else
		{
			flag = 0;
		}
		if (flag == 1)
		{
			i++;
			if (i == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				pHead = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);	// 分配内存
		}
	}
	free(pNew);					// 将多余的空间释放
	fclose(fp);					// 关闭文件指针
	return pHead;
}

PMAPS readmaps_bad()
{
	PMAPS pHead = NULL;
	PMAPS pNew = NULL;
	PMAPS pEnd = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	FILE *fp;
	int i = 0, flag = 1;
	char lj[64], buff[256];
	sprintf(lj, "/proc/%d/maps", iPID);
	fp = fopen(lj, "r");
	if (fp == NULL)
	{
		puts("内存分析错误");
		return NULL;
	}
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取一行
		if (strstr(buff, "rw") != NULL && !feof(fp) && strstr(buff, "kgsl-3d0"))
		{
			sscanf(buff, "%lx-%lx", &pNew->addr, &pNew->taddr);
			// 这里使用lx是为了能成功读取特别长的地址
			flag = 1;
		}
		else
		{
			flag = 0;
		}
		if (flag == 1)
		{
			i++;
			if (i == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				pHead = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);	// 分配内存
		}
	}
	free(pNew);					// 将多余的空间释放
	fclose(fp);					// 关闭文件指针
	return pHead;
}

PMAPS readmaps_c_alloc()
{
	PMAPS pHead = NULL;
	PMAPS pNew = NULL;
	PMAPS pEnd = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	FILE *fp;
	int i = 0, flag = 1;
	char lj[64], buff[256];
	sprintf(lj, "/proc/%d/maps", iPID);
	fp = fopen(lj, "r");
	if (fp == NULL)
	{
		puts("内存分析错误");
		return NULL;
	}
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取一行
		if (strstr(buff, "rw") != NULL && !feof(fp) && strstr(buff, "[anon:libc_malloc]"))
		{
			sscanf(buff, "%lx-%lx", &pNew->addr, &pNew->taddr);
			// 这里使用lx是为了能成功读取特别长的地址
			flag = 1;
		}
		else
		{
			flag = 0;
		}
		if (flag == 1)
		{
			i++;
			if (i == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				pHead = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);	// 分配内存
		}
	}
	free(pNew);					// 将多余的空间释放
	fclose(fp);					// 关闭文件指针
	return pHead;
}

PMAPS readmaps_c_bss()
{
	PMAPS pHead = NULL;
	PMAPS pNew = NULL;
	PMAPS pEnd = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	FILE *fp;
	int i = 0, flag = 1;
	char lj[64], buff[256];
	sprintf(lj, "/proc/%d/maps", iPID);
	fp = fopen(lj, "r");
	if (fp == NULL)
	{
		puts("内存分析错误");
		return NULL;
	}
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取一行
		if (strstr(buff, "rw") != NULL && !feof(fp) && strstr(buff, "[anon:.bss]"))
		{
			sscanf(buff, "%lx-%lx", &pNew->addr, &pNew->taddr);
			// 这里使用lx是为了能成功读取特别长的地址
			flag = 1;
		}
		else
		{
			flag = 0;
		}
		if (flag == 1)
		{
			i++;
			if (i == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				pHead = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);	// 分配内存
		}
	}
	free(pNew);					// 将多余的空间释放
	fclose(fp);					// 关闭文件指针
	return pHead;
}

PMAPS readmaps_c_data()
{
	PMAPS pHead = NULL;
	PMAPS pNew = NULL;
	PMAPS pEnd = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	FILE *fp;
	int i = 0, flag = 1;
	char lj[64], buff[256];
	sprintf(lj, "/proc/%d/maps", iPID);
	fp = fopen(lj, "r");
	if (fp == NULL)
	{
		puts("内存分析错误");
		return NULL;
	}
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取一行
		if (strstr(buff, "rw") != NULL && !feof(fp) && strstr(buff, "/data/app/"))
		{
			sscanf(buff, "%lx-%lx", &pNew->addr, &pNew->taddr);
			// 这里使用lx是为了能成功读取特别长的地址
			flag = 1;
		}
		else
		{
			flag = 0;
		}
		if (flag == 1)
		{
			i++;
			if (i == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				pHead = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);	// 分配内存
		}
	}
	free(pNew);					// 将多余的空间释放
	fclose(fp);					// 关闭文件指针
	return pHead;
}
int hunxiao(char* t){return 0;}
PMAPS readmaps_c_heap()
{
	PMAPS pHead = NULL;
	PMAPS pNew = NULL;
	PMAPS pEnd = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	FILE *fp;
	int i = 0, flag = 1;
	char lj[64], buff[256];
	sprintf(lj, "/proc/%d/maps", iPID);
	fp = fopen(lj, "r");
	if (fp == NULL)
	{
		puts("内存分析错误");
		return NULL;
	}
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取一行
		if (strstr(buff, "rw") != NULL && !feof(fp) && strstr(buff, "[heap]"))
		{
			sscanf(buff, "%lx-%lx", &pNew->addr, &pNew->taddr);
			// 这里使用lx是为了能成功读取特别长的地址
			flag = 1;
		}
		else
		{
			flag = 0;
		}
		if (flag == 1)
		{
			i++;
			if (i == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				pHead = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);	// 分配内存
		}
	}
	free(pNew);					// 将多余的空间释放
	fclose(fp);					// 关闭文件指针
	return pHead;
}

PMAPS readmaps_java_heap()
{
	PMAPS pHead = NULL;
	PMAPS pNew = NULL;
	PMAPS pEnd = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	FILE *fp;
	int i = 0, flag = 1;
	char lj[64], buff[256];
	sprintf(lj, "/proc/%d/maps", iPID);
	fp = fopen(lj, "r");
	if (fp == NULL)
	{
		puts("内存分析错误");
		return NULL;
	}
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取一行
		if (strstr(buff, "rw") != NULL && !feof(fp) && strstr(buff, "/dev/ashmem/"))
		{
			sscanf(buff, "%lx-%lx", &pNew->addr, &pNew->taddr);
			// 这里使用lx是为了能成功读取特别长的地址
			flag = 1;
		}
		else
		{
			flag = 0;
		}
		if (flag == 1)
		{
			i++;
			if (i == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				pHead = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);	// 分配内存
		}
	}
	free(pNew);					// 将多余的空间释放
	fclose(fp);					// 关闭文件指针
	return pHead;
}

PMAPS readmaps_a_anonmyous()
{
	PMAPS pHead = NULL;
	PMAPS pNew = NULL;
	PMAPS pEnd = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	FILE *fp;
	int i = 0, flag = 1;
	char lj[64], buff[256];
	sprintf(lj, "/proc/%d/maps", iPID);
	fp = fopen(lj, "r");
	if (fp == NULL)
	{
		puts("内存分析错误");
		return NULL;
	}
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取一行
		if (strstr(buff, "rw") != NULL && !feof(fp) && (strlen(buff) < 42))
		{
			sscanf(buff, "%lx-%lx", &pNew->addr, &pNew->taddr);
			// 这里使用lx是为了能成功读取特别长的地址
			flag = 1;
		}
		else
		{
			flag = 0;
		}
		if (flag == 1)
		{
			i++;
			if (i == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				pHead = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);	// 分配内存
		}
	}
	free(pNew);					// 将多余的空间释放
	fclose(fp);					// 关闭文件指针
	return pHead;
}

PMAPS readmaps_code_system()
{
	PMAPS pHead = NULL;
	PMAPS pNew = NULL;
	PMAPS pEnd = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	FILE *fp;
	int i = 0, flag = 1;
	char lj[64], buff[256];
	sprintf(lj, "/proc/%d/maps", iPID);
	fp = fopen(lj, "r");
	if (fp == NULL)
	{
		puts("内存分析错误");
		return NULL;
	}
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取一行
		if (strstr(buff, "rw") != NULL && !feof(fp) && strstr(buff, "/system"))
		{
			sscanf(buff, "%lx-%lx", &pNew->addr, &pNew->taddr);
			// 这里使用lx是为了能成功读取特别长的地址
			flag = 1;
		}
		else
		{
			flag = 0;
		}
		if (flag == 1)
		{
			i++;
			if (i == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				pHead = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);	// 分配内存
		}
	}
	free(pNew);					// 将多余的空间释放
	fclose(fp);					// 关闭文件指针
	return pHead;
}

PMAPS readmaps_stack()
{
	PMAPS pHead = NULL;
	PMAPS pNew = NULL;
	PMAPS pEnd = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	FILE *fp;
	int i = 0, flag = 1;
	char lj[64], buff[256];
	sprintf(lj, "/proc/%d/maps", iPID);
	fp = fopen(lj, "r");
	if (fp == NULL)
	{
		puts("内存分析错误");
		return NULL;
	}
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取一行
		if (strstr(buff, "rw") != NULL && !feof(fp) && strstr(buff, "[stack]"))
		{
			sscanf(buff, "%lx-%lx", &pNew->addr, &pNew->taddr);
			// 这里使用lx是为了能成功读取特别长的地址
			flag = 1;
		}
		else
		{
			flag = 0;
		}
		if (flag == 1)
		{
			i++;
			if (i == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				pHead = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);	// 分配内存
		}
	}
	free(pNew);					// 将多余的空间释放
	fclose(fp);					// 关闭文件指针
	return pHead;
}

PMAPS readmaps_ashmem()
{
	PMAPS pHead = NULL;
	PMAPS pNew = NULL;
	PMAPS pEnd = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	FILE *fp;
	int i = 0, flag = 1;
	char lj[64], buff[256];
	sprintf(lj, "/proc/%d/maps", iPID);
	fp = fopen(lj, "r");
	if (fp == NULL)
	{
		puts("内存分析错误");
		return NULL;
	}
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取一行
		if (strstr(buff, "rw") != NULL && !feof(fp) && strstr(buff, "/dev/ashmem/")
			&& !strstr(buff, "dalvik"))
		{
			sscanf(buff, "%lx-%lx", &pNew->addr, &pNew->taddr);
			// 这里使用lx是为了能成功读取特别长的地址
			flag = 1;
		}
		else
		{
			flag = 0;
		}
		if (flag == 1)
		{
			i++;
			if (i == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				pHead = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);	// 分配内存
		}
	}
	free(pNew);					// 将多余的空间释放
	fclose(fp);					// 关闭文件指针
	return pHead;
}

void Print()
{
	PMAPS temp = Res;
	int i;
	for (i = 0; i < ResCount; i++)
	{
		printf("No.%d Address: 0x%lX, Value: %d\n", i,temp->addr, temp->value);
		temp = temp->next;		// 指向下一个节点
	}
}
void Next()
{
		Res = Res->next;
}

void ClearResults()				// 清空
{
	PMAPS pHead = Res;
	PMAPS pTemp = pHead;
	int i;
	for (i = 0; i < ResCount; i++)
	{
		pTemp = pHead;
		pHead = pHead->next;
		free(pTemp);
	}
}

void BaseAddressSearch(char *value, COUNT * gs, TYPE type, ADDRESS BaseAddr)
{
	PMAPS pHead = NULL;
	PMAPS pMap = NULL;
	switch (MemorySearchRange)
	{
	case ALL: pMap = readmaps(ALL); break;
	case B_BAD: pMap = readmaps(B_BAD); break;
	case C_ALLOC: pMap = readmaps(C_ALLOC); break;
	case C_BSS: pMap = readmaps(C_BSS); break;
	case C_DATA: pMap = readmaps(C_DATA); break;
	case C_HEAP: pMap = readmaps(C_HEAP); break;
	case JAVA_HEAP: pMap = readmaps(JAVA_HEAP); break;
	case A_ANONMYOUS: pMap = readmaps(A_ANONMYOUS); break;
	case CODE_SYSTEM: pMap = readmaps(CODE_SYSTEM); break;
	case STACK: pMap = readmaps(STACK); break;
	case ASHMEM: pMap = readmaps(ASHMEM); break;
	default:
		printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
	if (pMap == NULL)
	{
		puts("初始化错误");
		return (void)0;
	}
	switch (type)
	{
	case DWORD: pHead = BaseAddressSearch_DWORD(atoi(value), gs, BaseAddr, pMap); break;
	case FLOAT: pHead = BaseAddressSearch_FLOAT(atof(value), gs, BaseAddr, pMap); break;
	default: printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
	if (pHead == NULL)
	{
		puts("search error");
		return (void)0;
	}
	ResCount = *gs;
	Res = pHead;				// Res指针指向链表
}

PMAPS BaseAddressSearch_DWORD(int value, COUNT * gs, ADDRESS BaseAddr, PMAPS pMap)
{
	*gs = 0;
	// printf("BaseAddress:%lX\n",BaseAddr);
	
	PMAPS e, n;
	e = n = (PMAPS) malloc(LEN);
	PMAPS pBuff = n;
	int iCount = 0;
	long int c, ADDR;
	int handle;
	char lj[64];
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);	// 打开mem文件
	lseek(handle, 0, SEEK_SET);
	void *BUF[8];
	PMAPS pTemp = pMap;
	while (pTemp != NULL)
	{
		c = (pTemp->taddr - pTemp->addr) / 4096;
		for (int j = 0; j < c; j++)
		{
			ADDR = pTemp->addr + j * 4096 + BaseAddr;
			pread64(handle, BUF, 8, ADDR);
			if (*(int *)&BUF[0] == value)
			{
				iCount++;
				*gs += 1;
				ResCount += 1;
				n->addr = ADDR;
				// printf("addr:%lx,val:%d,buff=%d\n",n->addr,value,buff[i]);
				if (iCount == 1)
				{
					n->next = NULL;
					e = n;
					pBuff = n;
				}
				else
				{
					n->next = NULL;
					e->next = n;
					e = n;
				}
				n = (PMAPS) malloc(LEN);
			}
		}
		pTemp = pTemp->next;
	}
	close(handle);
	return pBuff;
}

PMAPS BaseAddressSearch_FLOAT(float value, COUNT * gs, ADDRESS BaseAddr, PMAPS pMap)
{
	*gs = 0;
	
	PMAPS e, n;
	e = n = (PMAPS) malloc(LEN);
	PMAPS pBuff = n;
	long int c, ADDR;
	int handle;
	int iCount = 0;
	char lj[64];
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);	// 打开mem文件
	lseek(handle, 0, SEEK_SET);
	void *BUF[8];
	PMAPS pTemp = pMap;
	while (pTemp != NULL)
	{
		c = (pTemp->taddr - pTemp->addr) / 4096;
		for (int j = 0; j < c; j++)
		{
			ADDR = pTemp->addr + j * 4096 + BaseAddr;
			pread64(handle, BUF, 8, ADDR);
			if (*(float *)&BUF[0] == value)
			{
				iCount++;
				*gs += 1;
				ResCount += 1;
				n->addr = ADDR;
				// printf("addr:%lx,val:%d,buff=%d\n",n->addr,value,buff[i]);
				if (iCount == 1)
				{
					n->next = NULL;
					e = n;
					pBuff = n;
				}
				else
				{
					n->next = NULL;
					e->next = n;
					e = n;
				}
				n = (PMAPS) malloc(LEN);
			}
		}
		pTemp = pTemp->next;
	}
	close(handle);
	return pBuff;
}

void RangeMemorySearch(char *from_value, char *to_value, COUNT * gs, TYPE type)	// 范围搜索
{
	PMAPS pHead = NULL;
	PMAPS pMap = NULL;
	switch (MemorySearchRange)
	{
	case ALL: pMap = readmaps(ALL); break;
	case B_BAD: pMap = readmaps(B_BAD); break;
	case C_ALLOC: pMap = readmaps(C_ALLOC); break;
	case C_BSS: pMap = readmaps(C_BSS); break;
	case C_DATA: pMap = readmaps(C_DATA); break;
	case C_HEAP: pMap = readmaps(C_HEAP); break;
	case JAVA_HEAP: pMap = readmaps(JAVA_HEAP); break;
	case A_ANONMYOUS: pMap = readmaps(A_ANONMYOUS); break;
	case CODE_SYSTEM: pMap = readmaps(CODE_SYSTEM); break;
	case STACK: pMap = readmaps(STACK); break;
	case ASHMEM: pMap = readmaps(ASHMEM); break;
	default:
		printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
	if (pMap == NULL)
	{
		puts("初始化错误");
		return (void)0;
	}
	switch (type)
	{
	case DWORD:
		if (atoi(from_value) > atoi(to_value))
			pHead = RangeMemorySearch_DWORD(atoi(to_value), atoi(from_value), gs, pMap);
		else
			pHead = RangeMemorySearch_DWORD(atoi(from_value), atoi(to_value), gs, pMap); break;
	case FLOAT:
		if (atof(from_value) > atof(to_value))
			pHead = RangeMemorySearch_FLOAT(atof(to_value), atof(from_value), gs, pMap);
		else
			pHead = RangeMemorySearch_FLOAT(atof(from_value), atof(to_value), gs, pMap); break;
	default:
		printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
	if (pHead == NULL)
	{
		puts("RangeSearch Error");
		return (void)0;
	}
	ResCount = *gs;
	Res = pHead;				// Res指针指向链表
}

PMAPS RangeMemorySearch_DWORD(int from_value, int to_value, COUNT * gs, PMAPS pMap)	// DWORD
{
	
	*gs = 0;
	PMAPS pTemp = NULL;
	pTemp = pMap;
	PMAPS n, e;
	e = n = (PMAPS) malloc(LEN);
	PMAPS pBuff;
	pBuff = n;
	int handle;					// 句柄
	int iCount = 0;				// 链表长度
	int c;
	char lj[64];				// 路径
	int buff[1024] = { 0 };		// 缓冲区
	memset(buff, 0, 4);
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);	// 打开mem文件
	lseek(handle, 0, SEEK_SET);
	while (pTemp != NULL)		// 读取maps里面的地��
	{
		c = (pTemp->taddr - pTemp->addr) / 4096;
		for (int j = 0; j < c; j++)
		{
			pread64(handle, buff, 0x1000, pTemp->addr + j * 4096);
			for (int i = 0; i < 1024; i++)
			{
				if (buff[i] >= from_value && buff[i] <= to_value)	// 判断值是否在这两者之间
				{
					iCount++;
					*gs += 1;
					ResCount += 1;
					n->addr = (pTemp->addr) + (j * 4096) + (i * 4);
					if (iCount == 1)
					{
						n->next = NULL;
						e = n;
						pBuff = n;
					}
					else
					{
						n->next = NULL;
						e->next = n;
						e = n;
					}
					n = (PMAPS) malloc(LEN);
				}
			}
		}
		pTemp = pTemp->next;
	}
	free(n);
	close(handle);
	return pBuff;
}

PMAPS RangeMemorySearch_FLOAT(float from_value, float to_value, COUNT * gs, PMAPS pMap)	// FLOAT
{
	
	*gs = 0;
	PMAPS pTemp = NULL;
	pTemp = pMap;
	PMAPS n, e;
	e = n = (PMAPS) malloc(LEN);
	PMAPS pBuff;
	pBuff = n;
	int handle;					// 句柄
	int iCount = 0;				// 链表长度
	int c;
	char lj[64];				// 路径
	float buff[1024] = { 0 };	// 缓冲区
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);	// 打开mem文件
	lseek(handle, 0, SEEK_SET);
	while (pTemp->next != NULL)
	{
		c = (pTemp->taddr - pTemp->addr) / 4096;
		for (int j = 0; j < c; j += 1)
		{
			pread64(handle, buff, 0x1000, pTemp->addr + (j * 4096));
			for (int i = 0; i < 1024; i += 1)
			{
				if (buff[i] >= from_value && buff[i] <= to_value)	// 判断。。。
				{
					iCount++;
					*gs += 1;
					ResCount += 1;
					n->addr = (pTemp->addr) + (j * 4096) + (i * 4);
					if (iCount == 1)
					{
						n->next = NULL;
						e = n;
						pBuff = n;
					}
					else
					{
						n->next = NULL;
						e->next = n;
						e = n;
					}
					n = (PMAPS) malloc(LEN);
				}
				// printf("buff[%d]=%f\n",l,buff[l]);
				// usleep(1);
			}
			// memset(buff,0,4);
		}
		pTemp = pTemp->next;
	}
	free(n);
	close(handle);
	return pBuff;
}

void MemorySearch(char *value, int *gs, TYPE type)
{
	PMAPS pHead = NULL;
	PMAPS pMap = NULL;
	switch (MemorySearchRange)
	{
	case ALL: pMap = readmaps(ALL); break;
	case B_BAD: pMap = readmaps(B_BAD); break;
	case C_ALLOC: pMap = readmaps(C_ALLOC); break;
	case C_BSS: pMap = readmaps(C_BSS); break;
	case C_DATA: pMap = readmaps(C_DATA); break;
	case C_HEAP: pMap = readmaps(C_HEAP); break;
	case JAVA_HEAP: pMap = readmaps(JAVA_HEAP); break;
	case A_ANONMYOUS: pMap = readmaps(A_ANONMYOUS); break;
	case CODE_SYSTEM: pMap = readmaps(CODE_SYSTEM); break;
	case STACK: pMap = readmaps(STACK); break;
	case ASHMEM: pMap = readmaps(ASHMEM); break;
	default: printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
	if (pMap == NULL)
	{
		puts("初始化错误");
		return (void)0;
	}
	switch (type)
	{
	case DWORD: pHead = MemorySearch_DWORD(atoi(value), gs, pMap); break;
	case FLOAT: pHead = MemorySearch_FLOAT(atof(value), gs, pMap); break;
	case BYTE: pHead = MemorySearch_BYTE(atoi(value), gs, pMap); break;
	default: printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
	if (pHead == NULL)
	{
		puts("search error");
		return (void)0;
	}
	ResCount = *gs;
	Res = pHead;				// Res指针指向链表
}

PMAPS MemorySearch_DWORD(int value, COUNT * gs, PMAPS pMap)
{
	
	*gs = 0;
	PMAPS pTemp = NULL;
	pTemp = pMap;
	PMAPS n, e;
	e = n = (PMAPS) malloc(LEN);
	PMAPS pBuff;
	pBuff = n;
	int handle;					// 句柄
	int iCount = 0;				// 链表长度
	int c;
	char lj[64];				// 路径
	int buff[1024] = { 0 };		// 缓冲区
	memset(buff, 0, 4);
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);	// 打开mem文件
	lseek(handle, 0, SEEK_SET);
	while (pTemp != NULL)		// 读取maps里面的地址
	{
		c = (pTemp->taddr - pTemp->addr) / 4096;
		for (int j = 0; j < c; j++)
		{
			pread64(handle, buff, 0x1000, pTemp->addr + j * 4096);
			for (int i = 0; i < 1024; i++)
			{
				if (buff[i] == value)
				{
					iCount++;
					*gs += 1;
					ResCount += 1;
					n->addr = (pTemp->addr) + (j * 4096) + (i * 4);
					// printf("addr:%lx,val:%d,buff=%d\n",n->addr,value,buff[i]);
					if (iCount == 1)
					{
						n->next = NULL;
						e = n;
						pBuff = n;
					}
					else
					{
						n->next = NULL;
						e->next = n;
						e = n;
					}
					n = (PMAPS) malloc(LEN);
				}
			}
		}
		pTemp = pTemp->next;
	}
	free(n);
	close(handle);
	return pBuff;
}

PMAPS MemorySearch_FLOAT(float value, COUNT * gs, PMAPS pMap)
{
	
	*gs = 0;
	PMAPS pTemp = NULL;
	pTemp = pMap;
	PMAPS n, e;
	e = n = (PMAPS) malloc(LEN);
	PMAPS pBuff;
	pBuff = n;
	int handle;					// 句柄
	int iCount = 0;				// 链表长度
	int c;
	char lj[64];				// 路径
	float buff[1024] = { 0 };	// 缓冲区
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);	// 打开mem文件
	lseek(handle, 0, SEEK_SET);
	while (pTemp->next != NULL)
	{
		c = (pTemp->taddr - pTemp->addr) / 4096;
		for (int j = 0; j < c; j += 1)
		{
			pread64(handle, buff, 0x1000, pTemp->addr + (j * 4096));
			for (int i = 0; i < 1024; i += 1)
			{
				if (buff[i] == value)
				{
					iCount++;
					*gs += 1;
					ResCount += 1;
					n->addr = (pTemp->addr) + (j * 4096) + (i * 4);
					if (iCount == 1)
					{
						n->next = NULL;
						e = n;
						pBuff = n;
					}
					else
					{
						n->next = NULL;
						e->next = n;
						e = n;
					}
					n = (PMAPS) malloc(LEN);
				}
				// printf("buff[%d]=%f\n",l,buff[l]);
				// usleep(1);
			}
			// memset(buff,0,4);
		}
		pTemp = pTemp->next;
	}
	free(n);
	close(handle);
	return pBuff;
}

PMAPS MemorySearch_BYTE(int value, COUNT * gs, PMAPS pMap)
{
	
	*gs = 0;
	PMAPS pTemp = NULL;
	pTemp = pMap;
	PMAPS n, e;
	e = n = (PMAPS) malloc(LEN);
	PMAPS pBuff;
	pBuff = n;
	int handle;					// 句柄
	int iCount = 0;				// 链表长度
	int c;
	char lj[64];	// 路径
	int buff[1024] = { 0 };	// 缓冲区
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);	// 打开mem文件
	lseek(handle, 0, SEEK_SET);
	while (pTemp->next != NULL)
	{
		c = (pTemp->taddr - pTemp->addr) / 4096;
		for (int j = 0; j < c; j += 1)
		{
			pread64(handle, buff, 0x1000, pTemp->addr + (j * 4096));
			for (int i = 0; i < 1024; i += 1)
			{
				if (buff[i] == value) { iCount++; *gs += 1; ResCount += 1; n->addr = (pTemp->addr) + (j * 4096) + (i * 4); if (iCount == 1) { n->next = NULL; e = n; pBuff = n; } else { n->next = NULL; e->next = n; e = n; } n = (PMAPS) malloc(LEN); }
			}
		}
		pTemp = pTemp->next;
	}
	free(n);
	close(handle);
	return pBuff;
}

void MemoryOffset(char *value, OFFSET offset, COUNT * gs, TYPE type)
{
	PMAPS pHead = NULL;
	switch (type)
	{
	case DWORD: pHead = MemoryOffset_DWORD(atoi(value), offset, Res, gs); break;
	case FLOAT: pHead = MemoryOffset_FLOAT(atof(value), offset, Res, gs); break;
	case BYTE: pHead = MemoryOffset_BYTE(atoi(value), offset, Res, gs); break;
	default: printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
	if (pHead == NULL)
	{
		puts("offset error");
		return (void)0;
	}
	ResCount = *gs;				// 全局个数
	ClearResults();				// 清空存储的数据(释放空间)
	Res = pHead;				// 指向新搜索到的空间
}

PMAPS MemoryOffset_DWORD(int value, OFFSET offset, PMAPS pBuff, COUNT * gs)	// 搜索偏移
{
	
	*gs = 0;					// 初始个数为0
	PMAPS pEnd = NULL;
	PMAPS pNew = NULL;
	PMAPS pTemp = pBuff;
	PMAPS BUFF = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	BUFF = pNew;
	int iCount = 0, handle;		// 个数与句柄
	char lj[64];				// 路径
	long int all;				// 总和
	int *buf = (int *)malloc(sizeof(int));	// 缓冲区
	int jg;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	while (pTemp != NULL)
	{
		all = pTemp->addr + offset;
		pread64(handle, buf, 4, all);
		jg = *buf;
		if (jg == value)
		{
			iCount++;
			*gs += 1;
			pNew->addr = pTemp->addr;
			if (iCount == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				BUFF = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);
			if (ResCount == 1)
			{
				free(pNew);
				close(handle);
				return BUFF;
			}
		}
		/* else { printf("jg:%d,value:%d\n",jg,value); } */
		pTemp = pTemp->next;	// 指向下一个节点读取数据
	}
	free(pNew);
	close(handle);
	return BUFF;
}

PMAPS MemoryOffset_FLOAT(float value, OFFSET offset, PMAPS pBuff, COUNT * gs)	// 搜索偏移
{
	
	*gs = 0;					// 初始个数为0
	PMAPS pEnd = NULL;
	PMAPS pNew = NULL;
	PMAPS pTemp = pBuff;
	PMAPS BUFF = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	BUFF = pNew;
	int iCount = 0, handle;		// 个数与句柄
	char lj[64];				// 路径
	long int all;				// 总和
	float *buf = (float *)malloc(sizeof(float));	// 缓冲区
	// int buf[16]; //出现异常使用
	float jg;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	while (pTemp != NULL)
	{
		all = pTemp->addr + offset;	// 偏移后的地址
		pread64(handle, buf, 4, all);
		jg = *buf;
		if (jg == value)
		{
			iCount++;
			*gs += 1;
			// printf("偏移成功,addr:%lx\n",all);
			pNew->addr = pTemp->addr;
			if (iCount == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				BUFF = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);
			if (ResCount == 1)
			{
				free(pNew);
				close(handle);
				return BUFF;
			}
		}
		/* else { printf("jg:%e,value:%e\n",jg,value); } */
		pTemp = pTemp->next;	// 指向下一个节点读取数据
	}
	free(pNew);
	close(handle);
	return BUFF;
}

PMAPS MemoryOffset_BYTE(int value, OFFSET offset, PMAPS pBuff, COUNT * gs)	// 搜索偏移
{
	
	*gs = 0;					// 初始个数为0
	PMAPS pEnd = NULL;
	PMAPS pNew = NULL;
	PMAPS pTemp = pBuff;
	PMAPS BUFF = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	BUFF = pNew;
	int iCount = 0, handle;		// 个数与句柄
	char lj[64];				// 路径
	long int all;				// 总和
	int* buf = (int*)malloc(2);	// 缓冲区
	// int buf[16]; //出现异常使用
	int jg;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	while (pTemp != NULL)
	{
		all = pTemp->addr + offset;	// 偏移后的地址
		pread64(handle, buf, 2, all);
		jg = *buf;
		if (jg == value)
		{
			iCount++; *gs += 1; pNew->addr = pTemp->addr; if (iCount == 1) { pNew->next = NULL; pEnd = pNew; BUFF = pNew; } else { pNew->next = NULL; pEnd->next = pNew; pEnd = pNew; } pNew = (PMAPS) malloc(LEN); if (ResCount == 1) { free(pNew); close(handle); return BUFF; }
		}
		pTemp = pTemp->next;
	}
	free(pNew);
	close(handle);
	return BUFF;
}

void RangeMemoryOffset(char *from_value, char *to_value, OFFSET offset, COUNT * gs, TYPE type)	// 范围偏移
{
	PMAPS pHead = NULL;
	switch (type)
	{
	case DWORD:
		if (atoi(from_value) > atoi(to_value))
			pHead = RangeMemoryOffset_DWORD(atoi(to_value), atoi(from_value), offset, Res, gs);
		else
			pHead = RangeMemoryOffset_DWORD(atoi(from_value), atoi(to_value), offset, Res, gs); break;
	case FLOAT:
		if (atof(from_value) > atof(to_value))
			pHead = RangeMemoryOffset_FLOAT(atof(to_value), atof(from_value), offset, Res, gs);
		else
			pHead = RangeMemoryOffset_FLOAT(atof(from_value), atof(to_value), offset, Res, gs); break;
	default:
		printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
	if (pHead == NULL)
	{
		puts("RangeOffset error");
		return (void)0;
	}
	ResCount = *gs;				// 全局个数
	ClearResults();				// 清空��储的数据(释放空间)
	Res = pHead;				// 指向新搜索到的空间
}

PMAPS RangeMemoryOffset_DWORD(int from_value, int to_value, OFFSET offset, PMAPS pBuff, COUNT * gs)	// 搜索偏移DWORD
{
	
	*gs = 0;					// 初始个数为0
	PMAPS pEnd = NULL;
	PMAPS pNew = NULL;
	PMAPS pTemp = pBuff;
	PMAPS BUFF = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	BUFF = pNew;
	int iCount = 0, handle;		// 个数与句柄
	char lj[64];				// 路径
	long int all;				// 总和
	int *buf = (int *)malloc(sizeof(int));	// 缓冲区
	int jg;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	while (pTemp != NULL)
	{
		all = pTemp->addr + offset;
		pread64(handle, buf, 4, all);
		jg = *buf;
		if (jg >= from_value && jg <= to_value)
		{
			iCount++;
			*gs += 1;
			pNew->addr = pTemp->addr;
			if (iCount == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				BUFF = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);
			if (ResCount == 1)
			{
				free(pNew);
				close(handle);
				return BUFF;
			}
		}
		/* else { printf("jg:%d,value:%d\n",jg,value); } */
		pTemp = pTemp->next;	// 指向下一个节点读取数据
	}
	free(pNew);
	close(handle);
	return BUFF;
}

PMAPS RangeMemoryOffset_FLOAT(float from_value, float to_value, OFFSET offset, PMAPS pBuff, COUNT * gs)	// 搜索偏移FLOAT
{
	
	*gs = 0;					// 初始个数为0
	PMAPS pEnd = NULL;
	PMAPS pNew = NULL;
	PMAPS pTemp = pBuff;
	PMAPS BUFF = NULL;
	pEnd = pNew = (PMAPS) malloc(LEN);
	BUFF = pNew;
	int iCount = 0, handle;		// 个数与句柄
	char lj[64];				// 路径
	long int all;				// 总和
	float *buf = (float *)malloc(sizeof(float));	// 缓冲区
	// int buf[16]; //出现异常使用
	float jg;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	while (pTemp != NULL)
	{
		all = pTemp->addr + offset;	// 偏移后的地址
		pread64(handle, buf, 4, all);
		jg = *buf;
		if (jg >= from_value && jg <= to_value)
		{
			iCount++;
			*gs += 1;
			// printf("偏移成功,addr:%lx\n",all);
			pNew->addr = pTemp->addr;
			if (iCount == 1)
			{
				pNew->next = NULL;
				pEnd = pNew;
				BUFF = pNew;
			}
			else
			{
				pNew->next = NULL;
				pEnd->next = pNew;
				pEnd = pNew;
			}
			pNew = (PMAPS) malloc(LEN);
			if (ResCount == 1)
			{
				free(pNew);
				close(handle);
				return BUFF;
			}
		}
		/* else { printf("jg:%e,value:%e\n",jg,value); } */
		pTemp = pTemp->next;	// 指向下一个节点读取数据
	}
	free(pNew);
	close(handle);
	return BUFF;
}

uint32 base64_encode(const uint8 *text, uint32 text_len, uint8 *encode)
{
    uint32 i, j;
    for (i = 0, j = 0; i+3 <= text_len; i+=3)
    {
        encode[j++] = alphabet_map[text[i]>>2];                             //取出第一个字符的前6位并找出对应的结果字符
        encode[j++] = alphabet_map[((text[i]<<4)&0x30)|(text[i+1]>>4)];     //将第一个字符的后2位与第二个字符的前4位进行组合并找到对应的结果字符
        encode[j++] = alphabet_map[((text[i+1]<<2)&0x3c)|(text[i+2]>>6)];   //将第二个字符的后4位与第三个字符的前2位组合并找出对应的结果字符
        encode[j++] = alphabet_map[text[i+2]&0x3f];                         //取出第三个字符的后6位并找出结果字符
    }

    if (i < text_len)
    {
        uint32 tail = text_len - i;
        if (tail == 1)
        {
            encode[j++] = alphabet_map[text[i]>>2];
            encode[j++] = alphabet_map[(text[i]<<4)&0x30];
            encode[j++] = '=';
            encode[j++] = '=';
        }
        else //tail==2
        {
            encode[j++] = alphabet_map[text[i]>>2];
            encode[j++] = alphabet_map[((text[i]<<4)&0x30)|(text[i+1]>>4)];
            encode[j++] = alphabet_map[(text[i+1]<<2)&0x3c];
            encode[j++] = '=';
        }
    }
    return j;
}

uint32 base64_decode(const uint8 *code, uint32 code_len, uint8 *plain)
{
    assert((code_len&0x03) == 0);  //如果它的条件返回错误，则终止程序执行。4���倍数。

    uint32 i, j = 0;
    uint8 quad[4];
    for (i = 0; i < code_len; i+=4)
    {
        for (uint32 k = 0; k < 4; k++)
        {
            quad[k] = reverse_map[code[i+k]];//分组，每组四个分别依次转换为base64表内的十进制数
        }

        assert(quad[0]<64 && quad[1]<64);

        plain[j++] = (quad[0]<<2)|(quad[1]>>4); //取出第一个字符对应base64表的十进制数的前6位与第二个字符对应base64表的十进制数的前2位进行组合

        if (quad[2] >= 64)
            break;
        else if (quad[3] >= 64)
        {
            plain[j++] = (quad[1]<<4)|(quad[2]>>2); //取出第二个字符对应base64表的十进制数的后4位与第三个字符对应base64表的十进制数的前4位进行组合
            break;
        }
        else
        {
            plain[j++] = (quad[1]<<4)|(quad[2]>>2);
            plain[j++] = (quad[2]<<6)|quad[3];//取出第三个字符对应base64表的十进制数的后2位与第4个字符进行组合
        }
    }
    return j;
}

char * Base64_Dec(char* input)
{
	char *b;
	uint8 *text = (uint8 *) input;
	uint32 text_len = (uint32) strlen((char *)text);
	uint8 buffer[1024], buffer2[4096];
	uint32 size = base64_decode(text, text_len, buffer);
	buffer[size] = 0;
	sprintf(b,"%s", buffer);
	//printf("%s",b);
	return b;
}

void MemoryWrite(char *value, OFFSET offset, TYPE type)
{
	switch (type)
	{
	case DWORD: MemoryWrite_DWORD(atoi(value), Res, offset); break;
	case FLOAT: MemoryWrite_FLOAT(atof(value), Res, offset); break;
	default: printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
}

int MemoryWrite_DWORD(int value, PMAPS pBuff, OFFSET offset)
{
	
	PMAPS pTemp = NULL;
	char lj[64];
	int handle;
	pTemp = pBuff;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	int i;
	for (i = 0; i < ResCount; i++)
	{
		pwrite64(handle, &value, 4, pTemp->addr + offset);
		if (pTemp->next != NULL)
			pTemp = pTemp->next;
	}
	close(handle);
	return 0;
}

int MemoryWrite_FLOAT(float value, PMAPS pBuff, OFFSET offset)
{
	
	PMAPS pTemp = NULL;
	char lj[64];
	int handle;
	pTemp = pBuff;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	int i;
	for (i = 0; i < ResCount; i++)
	{
		pwrite64(handle, &value, 4, pTemp->addr + offset);
		if (pTemp->next != NULL)
			pTemp = pTemp->next;
	}
	close(handle);
	return 0;
}

void MemoryWrite_Fr(char *value, OFFSET offset, TYPE type)
{
	switch (type)
	{	
	//ttt++;
	//	case DWORD: std::thread t[ttt](MemoryWrite_DWORD_Fr,atoi(value), Res, offset); break;
	//	case FLOAT: std::thread t[ttt](MemoryWrite_FLOAT_Fr,atoi(value), Res, offset); break;
		case DWORD: MemoryWrite_DWORD_Fr(atoi(value), Res, offset); break;
		case FLOAT: MemoryWrite_FLOAT_Fr(atof(value), Res, offset); break;
		default: printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
}

int MemoryWrite_DWORD_Fr(int value, PMAPS pBuff, OFFSET offset)
{
	
	char lj[64];
	int handle;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	while (true)
	{
		PMAPS pTemp = NULL;
		pTemp = pBuff;
		int i;
		for (i = 0; i < ResCount; i++)
		{
			pwrite64(handle, &value, 4, pTemp->addr + offset);
			if (pTemp->next != NULL)
			pTemp = pTemp->next;
		}
	}
	close(handle);
	return 0;
}

int MemoryWrite_FLOAT_Fr(float value, PMAPS pBuff, OFFSET offset)
{
	char lj[64];
	int handle;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	while (true)
	{
		PMAPS pTemp = NULL;
		pTemp = pBuff;
		int i;
		for (i = 0; i < ResCount; i++)
		{
			pwrite64(handle, &value, 4, pTemp->addr + offset);
			if (pTemp->next != NULL)
			pTemp = pTemp->next;
		}
	}
	close(handle);
	return 0;
}

void *SearchAddress(ADDRESS addr)	// 返回一个void指针,可以自行转换类型
{
	char lj[64];
	int handle;
	void *buf = malloc(8);
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	pread64(handle, buf, 8, addr);
	close(handle);
	return buf;
}

void ReadAddress(ADDRESS addr, TYPE type)
{
	switch (type)
	{	
		case DWORD: GetAddress_DWORD(addr); break;
		case FLOAT: GetAddress_FLOAT(addr); break;
		default: printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
}

void GetAddress(ADDRESS addr, TYPE type)
{
	switch (type)
	{	
		case DWORD: GetAddress_DWORD(addr); break;
		case FLOAT: GetAddress_FLOAT(addr); break;
		default: printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
}

int GetAddress_DWORD(ADDRESS addr)
{
	char lj[64];
	int handle;
	long int all;
	int *buf = (int*)malloc(sizeof(int));
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	pread64(handle, buf, 4, addr);
	close(handle);
	return *buf;
}

float GetAddress_FLOAT(ADDRESS addr)
{
	char lj[64];
	int handle;
	long int all;
	float *buf = (float*)malloc(sizeof(float));
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	pread64(handle, buf, 4, addr);
	close(handle);
	return *buf;
}

char GetAddress_BYTE(ADDRESS addr)
{
	char lj[64];
	int handle;
	long int all;
	char buf[2] = "";
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	pread64(handle, buf, 2, addr);
	close(handle);
	return *buf;
}

int WriteAddress(ADDRESS addr, char *value, TYPE type) //写内存
{
	char lj[64];
	int handle;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	switch (type)
	{
	case DWORD:
		pwrite64(handle, (int *)value, 4, addr); break;
	case FLOAT:
		pwrite64(handle, (float *)value, 4, addr); break;
	default:
		printf("\033[32;1mYou Select A NULL Type!\n"); break;
	}
	close(handle);
	return 0;
}


int WriteAddress_FLOAT(ADDRESS addr, float value) //写内存
{
	char lj[64];
	int handle;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	lseek(handle, 0, SEEK_SET);
	pwrite64(handle, &value, 4, addr);
	close(handle);
	return 0;
}

int isapkinstalled(PACKAGENAME * bm)
{
	char LJ[128];
	sprintf(LJ, "/data/data/%s/", bm);
	DIR *dir;
	dir = opendir(LJ);
	if (dir == NULL)
	{
		return 0;
	}
	else
	{
		return 1;
	}
}

int isapkrunning(PACKAGENAME * bm)
{
	DIR *dir = NULL;
	struct dirent *ptr = NULL;
	FILE *fp = NULL;
	char filepath[50];			// 大小随意，能装下cmdline文件的路径即可
	char filetext[128];			// 大小随意，能装下要识别的命令行文本即可
	dir = opendir("/proc/");	// 打开路径
	if (dir != NULL)
	{
		while ((ptr = readdir(dir)) != NULL)	// 循环读取路径下的每一个文件/文件夹
		{
			// 如果读取到的是"."或者".."则跳过，读取到的不是文件夹名字也跳过
			if ((strcmp(ptr->d_name, ".") == 0) || (strcmp(ptr->d_name, "..") == 0))
				continue;
			if (ptr->d_type != DT_DIR)
				continue;
			sprintf(filepath, "/proc/%s/cmdline", ptr->d_name);	// 生成要读取的文件的路径
			fp = fopen(filepath, "r");	// 打开文件
			if (NULL != fp)
			{
				fgets(filetext, sizeof(filetext), fp);	// 读取文件
				if (strcmp(filetext, bm) == 0)
				{
					closedir(dir);
					return 1;
				}
				fclose(fp);
			}
		}
	}
	closedir(dir);				// 关闭路径
	return 0;
}

int uninstallapk(PACKAGENAME * bm)
{
	char ml[128];
	sprintf(ml, "pm uninstall %s", bm);
	system(ml);
	clrscr();
	return 0;
}

int installapk(char *lj)
{
	char ml[128];
	sprintf(ml, "pm install %s", lj);
	system(ml);
	clrscr();
	return 0;
}

int killprocess(PACKAGENAME * bm)
{
	int pid = getPID(bm);
	if (pid == 0)
	{
		return -1;
	}
	char ml[32];
	sprintf(ml, "kill %d", pid);
	system(ml);					// 杀掉进程
	return 0;
}

char GetProcessState(PACKAGENAME * bm)
{
	//   D 无法中断的休眠状态（通常 IO 的进程）； R
	//   正在运行，在可中断队列中； S
	//   处于休眠状态，静止状态； T
	//   停止或被追踪，暂停执行； W
	//   进入内存交换（从内核2.6开始无效）； X
	//   死掉的进程； Z 僵尸进程不存在但暂时无法消除； W:
	//   没有足够的记忆体分页可分配WCHAN
	//   正在等待的进程资源； <: 高优先级进程 N:
	//   低优先序进程 L: 有记忆体分页���配并锁在记忆体内
	//   (即时系统或捱A I/O)，即,有些页被锁进内存 s
	//   进程的领导者（在它之下有子进程）； l
	//   多进程的（使用 CLONE_THREAD, 类似 NPTL pthreads）； +
	//   位于后台的进程组； 
	int pid = getPID(bm);		// 获取pid
	if (pid == 0)
	{
		return 0;				// 无进程
	}
	FILE *fp;
	char lj[64];
	char buff[64];
	char zt;
	char zt1[16];
	sprintf(lj, "/proc/%d/status", pid);
	fp = fopen(lj, "r");		// 打开status文件
	if (fp == NULL)
	{
		return 0;				// 无进程
	}
	// puts("loop");
	while (!feof(fp))
	{
		fgets(buff, sizeof(buff), fp);	// 读取
		if (strstr(buff, "State"))	// 筛选
		{
			sscanf(buff, "State: %c", &zt);	// 选取
			// printf("state:%c\n",zt);
			// sleep(1);
			// puts("emmmm");
			break;				// 跳出循环
		}
	}
	// putchar(zt);
	// puts(zt2);
	fclose(fp);
	// puts("loopopp");
	return zt;					// 返回状态
}

int rebootsystem() { return system("su -c 'reboot'"); }
int PutDate() { return system("date +%F-%T"); }

int GetDate(char *date)
{
	FILE *fp;					// 文件指针
	system("date +%F-%T > log.txt");	// 执行
	if ((fp = fopen("log.txt", "r")) == NULL)
	{
		return 0;
	}
	fscanf(fp, "%s", date);		// 读取
	remove("log.txt");			// 删除
	return 1;
}

void BypassGameSafe() { system("echo 0 > /proc/sys/fs/inotify/max_user_watches"); }
void ReGameSafe() { system("echo 8192 > /proc/sys/fs/inotify/max_user_watches"); }

int killGG()					// 杀掉GG修改器
{
	// 在/data/data/[GG修改器包名]/files/里面有一个文件夹名字是GG-****
	// 如果有这个文件夹，就获取上面所说的包名，杀掉GG修改器
	DIR *dir = NULL;
	DIR *dirGG = NULL;
	struct dirent *ptr = NULL;
	struct dirent *ptrGG = NULL;
	char filepath[256];			// 大小随意，能装下cmdline文件的路径即可
	char filetext[128];			// 大小随意，能装下要识别的命令行文本即可
	dir = opendir("/data/data");	// 打开路径
	// puts("正在杀GG");
	int flag = 1;
	if (dir != NULL)
	{
		while (flag && (ptr = readdir(dir)) != NULL)	// 循环读取路径下的每一个文件/文件夹
		{
			// 如果读取到的是"."或者".."则跳过，读取到的不是文件夹名字也跳过
			if ((strcmp(ptr->d_name, ".") == 0) || (strcmp(ptr->d_name, "..") == 0))
				continue;
			if (ptr->d_type != DT_DIR)
				continue;
			sprintf(filepath, "/data/data/%s/files", ptr->d_name);	// 生成要读取的文件的路径
			dirGG = opendir(filepath);	// 打开文件
			if (dirGG != NULL)
			{
				while ((ptrGG = readdir(dirGG)) != NULL)
				{
					if ((strcmp(ptrGG->d_name, ".") == 0) || (strcmp(ptr->d_name, "..") == 0))
						continue;
					if (ptrGG->d_type != DT_DIR)
						continue;
					if (strstr(ptrGG->d_name, "GG"))	// 判断文件夹名字
					{
						int pid;	// pid
						pid = getPID(ptr->d_name);	// 获取GG包名
						// ptr->d_name存储文件名字(也就是软件包名)
						if (pid == 0)	// 如果pid是0，代表GG没有运行
							continue;
						else	// 如果成功获取pid
							killprocess(ptr->d_name);
					}
				}
			}
			/* else { puts(filepath);//调试 } */
		}
	}
	closedir(dir);				// 关闭
	closedir(dirGG);
	return 0;
}

int killXs()					// 杀掉Xs
{
	DIR *dir = NULL;
	struct dirent *ptr = NULL;
	char filepath[256];			// 大小随意，能装下cmdline文件的路径即可
	char filetext[128];			// 大小随意，能装下要识别的命令行文本即可
	dir = opendir("/data/data");	// 打开路径
	FILE *fp = NULL;
	if (NULL != dir)
	{
		while ((ptr = readdir(dir)) != NULL)	// 循环读取路径下的每一个文件/文件夹
		{
			// 如果读取到的是"."或者".."则跳过，读取到的不是文件夹名字也跳过
			if ((strcmp(ptr->d_name, ".") == 0) || (strcmp(ptr->d_name, "..") == 0))
				continue;
			if (ptr->d_type != DT_DIR)
				continue;
			// /data/data/%s/lib/libxscript.so
			sprintf(filepath, "/data/data/%s/lib/libxscript.so", ptr->d_name);	// 生成要读取的文件的路径
			fp = fopen(filepath, "r");
			if (fp == NULL)
				continue;
			else				// 如果读取成功(xs正在运行)
			{
				killprocess(ptr->d_name);
				// 杀进程
			}
			// killprocess(ptr->d_name);
		}
	}
	closedir(dir);				// 关闭
	return 0;
}

void *FreezeThread(void *a)
{
	int handle;
	int pid;
	char lj[64];
	int buf_i;
	float buf_f;
	sprintf(lj, "/proc/%d/mem", iPID);
	handle = open(lj, O_RDWR);
	if (handle == -1)
	{
		puts("Error -2");
		return (void *)0;
	}
	lseek(handle, 0, SEEK_SET);
	PFREEZE pTemp = Pfreeze;
	while (Freeze == 1)
	{
		for (int i = 0; i < FreezeCount; i++)
		{
			switch (pTemp->type)
			{
			case DWORD:
				buf_i = atoi(pTemp->value);
				pwrite64(handle, &buf_i, 4, pTemp->addr);
				break;
			case FLOAT:
				buf_f = atof(pTemp->value);
				pwrite64(handle, &buf_f, 4, pTemp->addr);
				break;
			default:
				break;
			}
			pTemp = pTemp->next;
			usleep(delay);
		}
		pTemp = Pfreeze;		// 重新指向头指针
	}
	return NULL;
}

PMAPS GetResults()				// 获取搜索出的结果
{
	if (Res == NULL)
	{
		return NULL;
	}
	else
	{
		return Res;				// 返回头指针
	}
}

int AddFreezeItem_All(char *Value, TYPE type, OFFSET offset)
{
	if (ResCount == 0)
	{
		return -1;
	}
	PMAPS pTemp = Res;
	for (int i = 0; i < ResCount; i++)
	{
		switch (type)
		{
		case DWORD:
			AddFreezeItem(pTemp->addr, Value, DWORD, offset);
			break;
		case FLOAT:
			AddFreezeItem(pTemp->addr, Value, FLOAT, offset);
			break;
		default:
			SetTextColor(COLOR_SKY_BLUE);
			puts("You Choose a NULL type");
			break;
		}
		pTemp = pTemp->next;
	}
	return 0;
}

int AddFreezeItem(ADDRESS addr, char *value, TYPE type, OFFSET offset)
{
	switch (type)
	{
	case DWORD:
		AddFreezeItem_DWORD(addr + offset, value); break;
	case FLOAT:
		AddFreezeItem_FLOAT(addr + offset, value); break;
	default:
		SetTextColor(COLOR_SKY_BLUE);
		puts("You Choose a NULL type"); break;
	}
	return 0;
}

int AddFreezeItem_DWORD(ADDRESS addr, char *value)
{
	if (FreezeCount == 0)		// 如果没有数据
	{
		Pfreeze = pEnd = pNew = (PFREEZE) malloc(FRE);	// 分配新空间
		pNew->next = NULL;
		pEnd = pNew;
		Pfreeze = pNew;
		pNew->addr = addr;		// 储存地���
		pNew->type = DWORD;
		pNew->value = value;
		FreezeCount += 1;
	}
	else
	{
		pNew = (PFREEZE) malloc(FRE);	// 分配空间
		pNew->next = NULL;
		pEnd->next = pNew;
		pEnd = pNew;
		pNew->addr = addr;
		pNew->type = DWORD;
		pNew->value = value;
		FreezeCount += 1;
	}
	return 0;
}

int AddFreezeItem_FLOAT(ADDRESS addr, char *value)
{
	if (FreezeCount == 0)		// 如果没有数据
	{
		Pfreeze = pEnd = pNew = (PFREEZE) malloc(FRE);	// 分配新空间
		pNew->next = NULL;
		pEnd = pNew;
		Pfreeze = pNew;
		pNew->addr = addr;		// 储存地址
		pNew->type = FLOAT;
		pNew->value = value;
		FreezeCount += 1;
	}
	else
	{
		pNew = (PFREEZE) malloc(FRE);	// 分配空间
		pNew->next = NULL;
		pEnd->next = pNew;
		pEnd = pNew;
		pNew->addr = addr;
		pNew->type = FLOAT;
		pNew->value = value;
		FreezeCount += 1;
	}
	return 0;
}

int RemoveFreezeItem(ADDRESS addr)
{
	PFREEZE pTemp = Pfreeze;
	PFREEZE p1 = NULL;
	PFREEZE p2 = NULL;
	for (int i = 0; i < FreezeCount; i++)
	{
		p1 = pTemp;
		p2 = pTemp->next;
		if (pTemp->addr == addr)
		{
			p1->next = p2;
			free(pTemp);
			FreezeCount -= 1;
			// printf("冻结个数:%d\n",FreezeCount);
			// break;//防止地址重复冻结，所以不加，当然也可以加上
		}
		pTemp = p2;
	}
	return 0;
}

int RemoveFreezeItem_All()
{
	PFREEZE pHead = Pfreeze;
	PFREEZE pTemp = pHead;
	int i;
	for (i = 0; i < FreezeCount; i++)
	{
		pTemp = pHead;
		pHead = pHead->next;
		free(pTemp);
		FreezeCount -= 1;
	}
	free(Pfreeze);
	FreezeCount -= 1;
	return 0;
}

int StartFreeze()
{
	if (Freeze == 1)			// 已经开启冻结
	{
		return -1;
	}
	int a;
	strcpy(Fbm, iPackage);			// ******
	Freeze = 1;
	pthread_create(&pth, NULL, FreezeThread, NULL);
	// 创建线程(开始冻结线程)
	return 0;
}

int StopFreeze()
{
	Freeze = 0;
	return 0;
}

int SetFreezeDelay(long int De)
{
	delay = De;
	return 0;
}

int PrintFreezeItems()
{
	PFREEZE pTemp = Pfreeze;
	for (int i = 0; i < FreezeCount; i++)
	{
		printf("FreezeAddr:%lx,type:%s,value:%s\n", pTemp->addr, pTemp->type == DWORD ? "DWORD" : "FLOAT", pTemp->value);
		pTemp = pTemp->next;
	}
	return 0;
}

void SetTar(char* pName) { SetTextColor(COLOR_SKY_BLUE); 
printf("作者:浮殇"); 
iPackage = pName; iPID = getPID(pName); 
}




