#ifndef ANDROIDP7ZIP_COMMAND_H
#define ANDROIDP7ZIP_COMMAND_H

#include <MyTypes.h>
#include "../7zip/CPP/myWindows/StdAfx.h"

#ifdef __cplusplus
extern "C" {
#endif

int MY_CDECL
main(
#ifndef _WIN32
        int numArgs, char *args[]
#endif
);

int executeCommand(const char *cmd);

#ifdef __cplusplus
}
#endif

#endif
