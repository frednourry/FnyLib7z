// MainAr.cpp

#include <android/log.h>
#include "StdAfx.h"

#include "../../../Common/MyException.h"
#include "../../../Common/StdOutStream.h"

#include "../../../Windows/ErrorMsg.h"
#include "../../../Windows/NtCheck.h"

#include "../Common/ArchiveCommandLine.h"
#include "../Common/ExitCode.h"

#include "ConsoleClose.h"

using namespace NWindows;

CStdOutStream *g_StdStream = NULL;
CStdOutStream *g_ErrStream = NULL;

extern int Main2(
  #ifndef _WIN32
  int numArgs, char *args[]
  #endif
);

static const char *kException_CmdLine_Error_Message = "Command Line Error:";
static const char *kExceptionErrorMessage = "ERROR:";
static const char *kUserBreakMessage  = "Break signaled";
static const char *kMemoryExceptionMessage = "ERROR: Can't allocate required memory!";
static const char *kUnknownExceptionMessage = "Unknown Error";
static const char *kInternalExceptionMessage = "\n\nInternal Error #";

static void FlushStreams()
{
  if (g_StdStream)
    g_StdStream->Flush();
}

static void PrintError(const char *message)
{
  FlushStreams();
  if (g_ErrStream)
    *g_ErrStream << "\n\n" << message << endl;
}

#define NT_CHECK_FAIL_ACTION *g_StdStream << "Unsupported Windows version"; return NExitCode::kFatalError;

int MY_CDECL main
(
  #ifndef _WIN32
  int numArgs, char *args[]
  #endif
)
{
  // FNY : Init the log files, so should retrieve the option -fny-stdout<output file> now
  for (int i=0; i<numArgs; i++) {
    int len = strlen(args[i]);
    char subString1 [12];
    if (len > 12) {
      strncpy(subString1, args[i], 11);
      if (strcmp(subString1, "-fny-stdout") == 0) {
        char subString[len];
        strncpy(subString, args[i]+11, len);
        __android_log_print(ANDROID_LOG_VERBOSE,"Main.cpp","output filename (-fny-stdout option)=%s", subString);
        g_StdOut.Open(subString);
      }
      else if (strcmp(subString1, "-fny-stderr") == 0) {
        char subString[len];
        strncpy(subString, args[i]+11, len);
        __android_log_print(ANDROID_LOG_VERBOSE,"Main.cpp","output filename (-fny-stderr option)=%s", subString);
        g_StdErr.Open(subString);
      }
    }
  }

   // END NOURRY

  g_ErrStream = &g_StdErr;
  g_StdStream = &g_StdOut;

  NT_CHECK

  NConsoleClose::CCtrlHandlerSetter ctrlHandlerSetter;
  int res = 0;
  
  try
  {
    res = Main2(
    #ifndef _WIN32
    numArgs, args
    #endif
    );
  }
  catch(const CNewException &)
  {
    PrintError(kMemoryExceptionMessage);
    g_StdErr.Close();
    return (NExitCode::kMemoryError);
  }
  catch(const NConsoleClose::CCtrlBreakException &)
  {
    PrintError(kUserBreakMessage);
    g_StdErr.Close();
    return (NExitCode::kUserBreak);
  }
  catch(const CArcCmdLineException &e)
  {
    PrintError(kException_CmdLine_Error_Message);
    if (g_ErrStream) {
      *g_ErrStream << e << endl;
      g_StdErr.Close();
    }
    return (NExitCode::kUserError);
  }
  catch(const CSystemException &systemError)
  {
    if (systemError.ErrorCode == E_OUTOFMEMORY)
    {
      PrintError(kMemoryExceptionMessage);
      g_StdErr.Close();
      return (NExitCode::kMemoryError);
    }
    if (systemError.ErrorCode == E_ABORT)
    {
      PrintError(kUserBreakMessage);
      g_StdErr.Close();
      return (NExitCode::kUserBreak);
    }
    if (g_ErrStream)
    {
      PrintError("System ERROR:");
      *g_ErrStream << NError::MyFormatMessage(systemError.ErrorCode) << endl;
      g_StdErr.Close();
    }
    return (NExitCode::kFatalError);
  }
  catch(NExitCode::EEnum &exitCode)
  {
    FlushStreams();
    if (g_ErrStream) {
      *g_ErrStream << kInternalExceptionMessage << exitCode << endl;
      g_StdErr.Close();
    }
    return (exitCode);
  }
  catch(const UString &s)
  {
    if (g_ErrStream)
    {
      PrintError(kExceptionErrorMessage);
      *g_ErrStream << s << endl;
      g_StdErr.Close();
    }
    return (NExitCode::kFatalError);
  }
  catch(const AString &s)
  {
    if (g_ErrStream)
    {
      PrintError(kExceptionErrorMessage);
      *g_ErrStream << s << endl;
      g_StdErr.Close();
    }
    return (NExitCode::kFatalError);
  }
  catch(const char *s)
  {
    if (g_ErrStream)
    {
      PrintError(kExceptionErrorMessage);
      *g_ErrStream << s << endl;
      g_StdErr.Close();
    }
    return (NExitCode::kFatalError);
  }
  catch(const wchar_t *s)
  {
    if (g_ErrStream)
    {
      PrintError(kExceptionErrorMessage);
      *g_ErrStream << s << endl;
      g_StdErr.Close();
    }
    return (NExitCode::kFatalError);
  }
  catch(int t)
  {
    if (g_ErrStream)
    {
      FlushStreams();
      *g_ErrStream << kInternalExceptionMessage << t << endl;
      g_StdErr.Close();
      return (NExitCode::kFatalError);
    }
  }
  catch(...)
  {
    PrintError(kUnknownExceptionMessage);
    g_StdErr.Close();
    return (NExitCode::kFatalError);
  }

  g_StdOut.Close();

  return res;
}
