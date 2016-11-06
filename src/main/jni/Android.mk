LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=\
	./src/native/JNIContext.cpp \
	./src/native/com_jupiter_snifferframework_Sniffer.cpp \
	./src/native/PacketReceiver.cpp \
	./src/Packet.cpp \
	./src/ipc/sema.cpp \
	./src/path.cpp


LOCAL_CFLAGS:=-O2 -g -fpermissive
LOCAL_CFLAGS+=-DHAVE_CONFIG_H -D_U_="__attribute__((unused))" -Dlinux -D__GLIBC__ -D_GNU_SOURCE -pie -fPIE
LOCAL_C_INCLUDES:=$(LOCAL_PATH)/include
LOCAL_C_INCLUDES+=$(LOCAL_PATH)/include/libnids
LOCAL_LDLIBS := -llog
LOCAL_LDFLAGS:=-pie -fPIE -shared

LOCAL_MODULE:= libpcap

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:=\
	bpf_dump.c\
	bpf/net/bpf_filter.c\
	bpf_image.c\
	etherent.c\
	fad-gifc.c\
	gencode.c\
	grammar.c\
	inet.c\
	nametoaddr.c\
	optimize.c\
	pcap.c\
	pcap-linux.c\
	savefile.c\
	scanner.c\
	version.c \
	main.cpp \
	./src/sniffer/Sniffer.cpp \
	./src/ipc/sema.cpp \
	./src/libnids/checksum.c \
	./src/libnids/hash.c \
	./src/libnids/ip_fragment.c \
	./src/libnids/tcp.c \
	./src/libnids/scan.c \
	./src/libnids/libnids.c \
	./src/libnids/ip_options.c \
	./src/utils.cpp \
	./src/Packet.cpp \
	./src/path.cpp


LOCAL_CFLAGS:=-O2 -g -fpermissive
LOCAL_CFLAGS+=-DHAVE_CONFIG_H -D_U_="__attribute__((unused))" -Dlinux -D__GLIBC__ -D_GNU_SOURCE -pie -fPIE
LOCAL_C_INCLUDES:=$(LOCAL_PATH)/include
LOCAL_C_INCLUDES+=$(LOCAL_PATH)/include/libnids
LOCAL_LDLIBS:=-llog
LOCAL_LDFLAGS:=-pie -fPIE

LOCAL_MODULE:= pcapd
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:=\
    ./src/native/com_jupiter_mail_MailInterface.cpp \
    ./src/mail/mail.cpp

LOCAL_C_INCLUDES:=$(LOCAL_PATH)/include
LOCAL_CFLAGS:=-fvisibility=hidden
LOCAL_LDLIBS:=-llog
LOCAL_MODULE:= libmail
include $(BUILD_SHARED_LIBRARY)

