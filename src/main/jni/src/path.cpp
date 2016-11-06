//
// Created by wangqiang on 16/11/1.
//
#include "path.h"

const char *Path::PUT   = "put";
const char *Path::GET   = "get";
const char *Path::MUTEX = "mutex";
const char *Path::SHM   = "shm";

char Path::sema_put_file_path[256] = {0};
char Path::sema_get_file_path[256] = {0};
char Path::sema_mutex_file_path[256] = {0};
char Path::shm_file_path[256] = {0};
