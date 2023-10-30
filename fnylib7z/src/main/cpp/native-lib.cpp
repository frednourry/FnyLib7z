#include <jni.h>
#include <string>
#include <android/log.h>

#include <7zip/MyVersion.h>
#include <cmd/command.h>
/*
// Example FileDescriptor : https://www.kfu.com/~nsayer/Java/jni-filedesc.html
int jniGetFDFromFileDescriptor(JNIEnv * env, jobject fileDescriptor) {
    // NOT USEFUL !!

    jint fd = -1;
    jclass fdClass = env->FindClass("java/io/FileDescriptor");

    if (fdClass != NULL) {
        jfieldID fdClassDescriptorFieldID = env->GetFieldID(fdClass, "descriptor", "I");
        if (fdClassDescriptorFieldID != NULL && fileDescriptor != NULL) {
            fd = env->GetIntField(fileDescriptor, fdClassDescriptorFieldID);
        }
    }

    return fd;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_frednourry_FnyLib7z_00024Companion_stringFromJNIWriteTempFile(JNIEnv *env, jobject thiz,
                                                                           jstring path) {
    int response = 0;

    // Path is an jstring passed as an arg of a function
    const char *charPath = (*env).GetStringUTFChars( path , 0 ) ;
    __android_log_print(ANDROID_LOG_VERBOSE, "libfnyLib7z.so", "fopen(%s)", charPath);

    FILE* file = fopen(charPath,"ab");
    if (file != NULL) {
        (*env).ReleaseStringUTFChars( path , charPath );
        int isWriting = fputs("Testing!\n", file);
        response = (isWriting>=0);
        __android_log_print(ANDROID_LOG_VERBOSE, "libfnyLib7z.so", "isWriting = %d", response);
        fclose(file);
    }

    return (jboolean) response;
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_github_frednourry_FnyLib7z_00024Companion_intFromJNIGetFDFileSize(JNIEnv *env, jobject thiz, jint fd) {
    // WORK !!

    if (fd < 0) {
        __android_log_print(ANDROID_LOG_WARN, "libfnyLib7z.so", "fd < 0 !");
        return (jint)-1;
    } else {
        // For test, try to read some characters...
        FILE* p_file = fdopen(fd, "r");
        if (p_file != NULL) {
            __android_log_print(ANDROID_LOG_VERBOSE, "libfnyLib7z.so", "file opened");
            char buf[6];
            int i = fread(buf, sizeof (buf), 1, p_file);
            __android_log_print(ANDROID_LOG_VERBOSE, "libfnyLib7z.so", "   i=%d  buf=%s", i, buf);

            fseek(p_file,0,SEEK_END);
            int size = ftell(p_file);
            fclose(p_file);
            return (jint) size;
        } else {
            __android_log_print(ANDROID_LOG_VERBOSE, "libfnyLib7z.so", "can't open file");
            return (jint) -1;
        }
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_github_frednourry_FnyLib7z_00024Companion_intFromJNIGetUriFileSize(JNIEnv *env, jobject thiz, jstring path) {
    // DOESN'T WORK !!!
    const char *thisPath = (*env).GetStringUTFChars(path, 0);
    __android_log_print(ANDROID_LOG_VERBOSE, "libfnyLib7z.so", "trying opening file: %s", thisPath);

    FILE *p_file = fopen(thisPath,"r");
    if (p_file != NULL) {
        __android_log_print(ANDROID_LOG_VERBOSE, "libfnyLib7z.so", "file opened: %s", thisPath);
        fseek(p_file,0,SEEK_END);
        int size = ftell(p_file);
        fclose(p_file);
        return (jint) size;
    } else {
        __android_log_print(ANDROID_LOG_VERBOSE, "libfnyLib7z.so", "  file not open...");
    }
    return (jint) -1;

    // Doesn't work better...
//    std::ifstream file( thisPath, std::ios::binary | std::ios::ate);
//    return (jint) file.tellg();
}
*/

extern "C"
JNIEXPORT jstring JNICALL
Java_io_github_frednourry_FnyLib7z_00024Companion_get7zVersionInfo(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(MY_VERSION);
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_github_frednourry_FnyLib7z_00024Companion_executeCommand7z(JNIEnv *env, jobject thiz, jstring command_) {
    const char *command = env->GetStringUTFChars(command_, nullptr);
    __android_log_print(ANDROID_LOG_VERBOSE,"libfnyLib7z.so","CMD:[%s]", command);
    int ret = executeCommand(command);
    env->ReleaseStringUTFChars(command_, command);
    return (jint) ret;
}

