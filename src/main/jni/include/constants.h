#ifndef __CONSTANTS_H__
#define __CONSTANTS_H__


#define LOCAL_SOCKET_PATH "sniffer_socket"


#define MAX_BUFF_SIZE (1024 * 1024)

//size of shared memory buffer size, each buffer unit is 64K,
//so total size of shared memory size is 2MB
#define SHM_BUFFER_SIZE 32

#endif
