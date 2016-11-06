//
// Created by wangqiang on 16/11/1.
//

#ifndef SNIFFER_PATH_H
#define SNIFFER_PATH_H

struct Path {
    static const char *PUT;
    static const char *GET;
    static const char *MUTEX;
    static const char *SHM;

    static char sema_put_file_path[256];
    static char sema_get_file_path[256];
    static char sema_mutex_file_path[256];
    static char shm_file_path[256];
};

#endif //SNIFFER_PATH_H
