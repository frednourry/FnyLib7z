package io.github.frednourry

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

data class itemListFnyLib7z(val name:String,
                            val size:Int,
                            val compressedSize:Int,
                            val isDir:Boolean,
                            val isReadOnly:Boolean,
                            val isHidden:Boolean,
                            val isSystem:Boolean,
                            val isArchive:Boolean,
                            val date:Date) {
    override fun toString(): String {
        return "$date $isDir $size $compressedSize $name"
    }

}


class FnyLib7z private constructor() {

    companion object {
        private const val TAG = "FnyLib7z"

        private const val tempDirName = "fnyLib7zTemp"

        const val RESULT_OK                     = 0
        const val RESULT_FALSE                  = 1
        const val RESULT_NOTIMPL                = -2147467263
        const val RESULT_NOINTERFACE            = -2147467262
        const val RESULT_ABORT                  = -2147467260
        const val RESULT_FAIL                   = -2147467259
        const val RESULT_INVALIDFUNCTION        = -2147287039
        const val RESULT_OUTOFMEMORY            = -2147024882
        const val RESULT_INVALIDARG             = -2147024809
        const val RESULT_LIBRARY_NOT_INIT       = -101
        const val RESULT_ARCHIVE_NOT_EXISTING   = -100
        const val RESULT_UNEXPECTED_ERROR       = -102
        const val RESULT_PARCEL_DESC_ERROR      = -103

        @Volatile
        private var instance: FnyLib7z? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: FnyLib7z().also { instance = it }
            }

        // Used to load the 'mylib7z' library on application startup.
        init {
            System.loadLibrary("fnyLib7z")
        }

        val resultMessage = mutableMapOf(
            RESULT_OK                       to "ok",
            RESULT_FALSE                    to "7zip: false",
            RESULT_NOTIMPL                  to "7zip: not implemented",
            RESULT_NOINTERFACE              to "7zip: no interface",
            RESULT_ABORT                    to "7zip: aborted",
            RESULT_FAIL                     to "7zip: failed",
            RESULT_INVALIDFUNCTION          to "7zip: invalid function",
            RESULT_OUTOFMEMORY              to "7zip: out of memory",
            RESULT_INVALIDARG               to "7zip: invalid argument",
            RESULT_LIBRARY_NOT_INIT         to "FnyLib7z: library 7zip not initialized",
            RESULT_ARCHIVE_NOT_EXISTING     to "FnyLib7z: archive file not found" ,
            RESULT_UNEXPECTED_ERROR         to "FnyLib7z: unexpected error",
            RESULT_PARCEL_DESC_ERROR        to "FnyLib7z: can't get parcel descriptor for the given uri"
        )
        fun getResultMessage(code:Int):String {
            return if (resultMessage.containsKey(code))
                resultMessage[code]!!
            else
                "unknown error"
        }

        /**
         * A native method that is implemented by the 'mylib7z' native library,
         * which is packaged with this application.
         */
//        external fun stringFromJNIWriteTempFile(path: String): Boolean  // For Test : Write a file in cache : Work

//        external fun intFromJNIGetFDFileSize(fd:Int):Int                // For Test : Work !

//        external fun intFromJNIGetUriFileSize(path:String):Int          // For Test : Doesn't work !

        external fun get7zVersionInfo():String

        private external fun executeCommand7z(command:String):Int
    }

    private lateinit var context:Context
    private lateinit var tempDirectory:String
    private lateinit var emptyDate:Date         // Define an empty date in case a file doesn't have one

    fun initialize(c:Context) {
        context = c

        // Set the empty date
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        emptyDate = dateFormatter.parse("1900-01-01 00:00:00")!!

        // Create the temp dir if not exists
        val tempDir = File(context.cacheDir.absolutePath+File.separator+tempDirName)
        Log.v(TAG, "initialize:: tempDir=${tempDir.absolutePath}")
        if (!tempDir.exists()) {
            if (tempDir.mkdirs()) {
                tempDirectory = tempDir.absolutePath
            } else {
                Log.w(TAG, "initialize:: can't create temp directory ${tempDir.absolutePath}")
            }
        } else {
            tempDirectory = tempDir.absolutePath
        }
    }

    fun isInitialized():Boolean {
        return ::context.isInitialized && ::tempDirectory.isInitialized
    }

    fun execute(paramsString:String, stdOutputPath:String = "", stdErrPath:String=""):Int {
        var command = String.format("7z $paramsString")
        if (stdOutputPath != "") {
            command += " '-fny-stdout${stdOutputPath}'"
        }
        if (stdErrPath != "") {
            command += " '-fny-stderr${stdErrPath}'"
        }

        return executeCommand7z(command)
    }

    // List archive content
    fun listFiles(path: String, filtersList:List<String> = emptyList(), sortList:Boolean = false, stdOutputPath:String = "", stdErrPath:String="", extraArgs:String=""):Int {
        Log.v(TAG, "listFiles:: path=$path")

        var commandParams = String.format("l '$path'")

        if (sortList) {
            commandParams += " -fny-sortlist"
        }
        if (extraArgs.isNotEmpty()) {
            commandParams += " $extraArgs"
        }

        if (filtersList.isNotEmpty()) {
            var params = " -r"
            for (filter in filtersList) {
                params += " '$filter'"
            }
            commandParams += params
        }
        Log.v(TAG, "listFiles:: command=$commandParams")

        return execute(commandParams, stdOutputPath=stdOutputPath, stdErrPath=stdErrPath)
    }

    fun listFiles(file: File, filtersList:List<String> = emptyList(), sortList:Boolean = false, stdOutputPath:String = "", stdErrPath:String="", extraArgs:String=""):Int {
        Log.v(TAG, "listFiles:: file=$file")
        if (file.exists())
            return listFiles(file.absolutePath, filtersList=filtersList, sortList=sortList, stdOutputPath=stdOutputPath, stdErrPath=stdErrPath, extraArgs=extraArgs)
        else
            return RESULT_ARCHIVE_NOT_EXISTING
    }

    /**
     * List the files (not directory) in an archive using its uri (should give its true path too !)
     */
    fun listFiles(uri: Uri, filtersList:List<String> = emptyList(), sortList:Boolean = false, stdOutputPath:String = "", stdErrPath:String=""):Int {
        Log.v(TAG, "listFiles:: uri=$uri")
        if (!isInitialized()) {
            Log.v(TAG, "listFiles:: library not initialized. Should call initialize(context) first!")
            return RESULT_LIBRARY_NOT_INIT
        }

        val uselessFilename = getNameFromUriLastPathSegment(uri, "myArchiveName.arc")

        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        parcelFileDescriptor?.let {
//            val fd = parcelFileDescriptor.fd
            val fd = parcelFileDescriptor.detachFd()

            val result = listFiles(uselessFilename, filtersList=filtersList, sortList=sortList, stdOutputPath=stdOutputPath, stdErrPath=stdErrPath, extraArgs="-fny-fdin$fd")
            parcelFileDescriptor.close()

            return result
        }
        return RESULT_PARCEL_DESC_ERROR;
    }

    fun parseListFile(stdoutFilePath:String):List<itemListFnyLib7z> = parseListFile(File(stdoutFilePath))

    fun parseListFile(stdoutFile: File):List<itemListFnyLib7z> {
        val mutList = mutableListOf<itemListFnyLib7z>()
        val lineSeparator = "------------------- ----- ------------ ------------  ------------------------"
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        var isParsing = false
        stdoutFile.forEachLine { line ->
            if (line == lineSeparator && !isParsing) {
                isParsing = true
            } else if (isParsing) {
                if (line == lineSeparator) {
                    isParsing = false
                } else {
//                    Log.v(TAG, line)
                    val strDate = line.substring(IntRange(0, 18))
                    val strAttr = line.substring(IntRange(20, 24))
                    val strSize = line.substring(IntRange(26, 37))
                    val strCompressedSize = line.substring(IntRange(39, 50))
                    val strName = line.substring(53)

                    val date = try {
                        if (strDate.trim() != "")
                            dateFormatter.parse(strDate)!!
                        else
                            emptyDate
                    } catch(e:Exception) {
                        emptyDate
                    }

                    mutList.add(
                        itemListFnyLib7z(
                            name=strName,
                            isDir = (strAttr[0]=='D'),
                            isReadOnly = (strAttr[1]=='R'),
                            isHidden = (strAttr[2]=='H'),
                            isSystem = (strAttr[3]=='S'),
                            isArchive = (strAttr[4]=='A'),
                            size = strSize.trim().toInt(),
                            compressedSize = strCompressedSize.trim().toInt(),
                            date = date
                        )
                    )
                }
            }
        }

        return mutList
    }

    // Create an archive
    fun compressFiles(path:String, filtersList: List<String>, format:String="zip", caseSensitive:Boolean=false, stdOutputPath:String = "", stdErrPath:String=""):Int {
        Log.v(TAG, "compressFiles:: path=$path")
        if (!isInitialized()) {
            Log.v(TAG, "compressFiles:: library not initialized. Should call initialize(context) first!")
            return RESULT_LIBRARY_NOT_INIT
        }

        var result = -1;
        var commandParams = String.format("a '${path}' -y -t$format -aoa '-w$tempDirectory'")

        if (caseSensitive) {
            commandParams += " -ssc"
        } else {
            commandParams += " -ssc-"
        }

        if (filtersList.isNotEmpty()) {
            var params = " -r" // ?
            for (p in filtersList) {
                params += " '$p'"
            }
            commandParams += params
        }
        Log.v(TAG, "compressFilesInFile:: command=$commandParams")

        result = execute(commandParams, stdOutputPath=stdOutputPath, stdErrPath=stdErrPath)

        return result
    }

    fun compressFiles(file:File, filtersList: List<String>, format:String="zip", caseSensitive:Boolean=false, stdOutputPath:String = "", stdErrPath:String=""):Int {
        Log.v(TAG, "compressFiles:: file=$file")
        // Test if file already exists?
        return compressFiles(file.absolutePath, filtersList=filtersList, format=format, caseSensitive=caseSensitive, stdOutputPath=stdOutputPath, stdErrPath=stdErrPath)
    }

    // Uncompress an archive
    fun uncompress(path: String, dirToExtract: File, filtersList:List<String> = emptyList(), caseSensitive:Boolean=false, numListToExtract:List<Int> = emptyList(), stdOutputPath:String = "", stdErrPath:String="", extraArgs:String=""):Int {
        Log.v(TAG, "uncompress:: path=$path")
        var commandParams = String.format("e '$path' '-o${dirToExtract.absolutePath}' -aoa")

        if (caseSensitive) {
            commandParams += " -ssc"
        } else {
            commandParams += " -ssc-"
        }

        if (numListToExtract.isNotEmpty()) {
            commandParams += " '-fny-n"+numListToExtract.joinToString(",")+"'"
        }
        if (filtersList.isNotEmpty()) {
            var params = " -r"
            for (filter in filtersList) {
                params += " '$filter'"
            }
            commandParams += params
        }
        if (extraArgs.isNotEmpty()) {
            commandParams += " $extraArgs"
        }

        Log.v(TAG, "uncompress:: command=$commandParams")
        return execute(commandParams, stdOutputPath=stdOutputPath, stdErrPath=stdErrPath)
    }

    fun uncompress(file: File, dirToExtract: File, caseSensitive:Boolean=false, numListToExtract:List<Int> = emptyList(), stdOutputPath:String = "", stdErrPath:String="", filtersList:List<String> = emptyList()):Int {
        Log.v(TAG, "uncompress:: file=$file")
        if (file.exists()) {
            return uncompress(file.absolutePath, dirToExtract=dirToExtract, filtersList=filtersList, caseSensitive=caseSensitive, stdOutputPath=stdOutputPath, stdErrPath=stdErrPath, numListToExtract=numListToExtract)
        } else {
            return RESULT_ARCHIVE_NOT_EXISTING
        }
    }

    fun uncompress(uri:Uri, dirToExtract: File, filtersList:List<String> = emptyList(), caseSensitive:Boolean=false, numListToExtract:List<Int> = emptyList(), stdOutputPath:String = "", stdErrPath:String=""):Int {
        Log.v(TAG, "uncompress:: uri=$uri")
        if (!isInitialized()) {
            Log.v(TAG, "uncompress:: library not initialized. Should call initialize(context) first!")
            return RESULT_LIBRARY_NOT_INIT
        }

        val uselessFilename = getNameFromUriLastPathSegment(uri, "myArchiveName.arc")

        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        parcelFileDescriptor?.let {
//            val fd = parcelFileDescriptor.fd
            val fd = parcelFileDescriptor.detachFd()
            val result = uncompress(uselessFilename, dirToExtract=dirToExtract, filtersList=filtersList, caseSensitive=caseSensitive, stdOutputPath=stdOutputPath, stdErrPath=stdErrPath, numListToExtract=numListToExtract, extraArgs = " -fny-fdin$fd")

            parcelFileDescriptor.close()

            return result
        }
        return RESULT_PARCEL_DESC_ERROR;
    }

    // Delete file(s) in an archive
    fun deleteInArchive(path:String, filtersList: List<String>, caseSensitive:Boolean=false, stdOutputPath:String = "", stdErrPath:String="", extraArgs:String=""):Int {
        Log.v(TAG, "deleteInArchive:: path=$path")
        var commandParams = String.format("d '$path' '-w$tempDirectory' '-o$tempDirectory'")

        if (caseSensitive) {
            commandParams += " -ssc"
        } else {
            commandParams += " -ssc-"
        }

        if (filtersList.isNotEmpty()) {
            var params = " -r"
            for (filter in filtersList) {
                params += " '$filter'"
            }
            commandParams += params
        }
        if (extraArgs.isNotEmpty()) {
            commandParams += " $extraArgs"
        }
        Log.v(TAG, "deleteInArchive:: command=$commandParams")

        return execute(commandParams, stdOutputPath=stdOutputPath, stdErrPath=stdErrPath)
    }

    fun deleteInArchive(file:File, filtersList: List<String>, caseSensitive:Boolean=false, stdOutputPath:String = "", stdErrPath:String=""):Int {
        if (file.exists()) {
            return deleteInArchive(file.absolutePath, filtersList=filtersList, caseSensitive=caseSensitive, stdOutputPath=stdOutputPath, stdErrPath=stdErrPath)
        } else {
            return RESULT_ARCHIVE_NOT_EXISTING
        }
    }

    fun deleteInArchive(uri:Uri, filtersList: List<String>, caseSensitive:Boolean=false, stdOutputPath:String = "", stdErrPath:String=""):Int {
        Log.v(TAG, "deleteInArchive:: uri=$uri")
        if (!isInitialized()) {
            Log.v(TAG, "deleteInArchive:: library not initialized. Should call initialize(context) first!")
            return RESULT_LIBRARY_NOT_INIT
        }

        var result = RESULT_PARCEL_DESC_ERROR

        val uselessFilename = getNameFromUriLastPathSegment(uri, "myArchiveName.arc")

        // Define a temp outfile (that doesn't exist)
        val tempOutputArchiveName = "archive.zip"
        val tempOutputArchiveFile = File(tempDirectory+File.separator+tempOutputArchiveName)
        if (tempOutputArchiveFile.exists())
            tempOutputArchiveFile.delete()

        // Open the input archive
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        parcelFileDescriptor?.let {
//            val fd = parcelFileDescriptor.fd
            val fd = parcelFileDescriptor.detachFd()

            result = deleteInArchive(uselessFilename, filtersList=filtersList, caseSensitive=caseSensitive, stdOutputPath=stdOutputPath, stdErrPath=stdErrPath, extraArgs = "-fny-fdin$fd -fny-tempoutfile$tempOutputArchiveName")

            parcelFileDescriptor.close()
        }

        // If ok, overwrite the URI content with the temp file content
        if (result == 0 && tempOutputArchiveFile.exists()) {
            // The temp archive was created and is ok. We just need to move it into 'uri'
            //            DocumentsContract.renameDocument(context.contentResolver, uri, file...)
            result = copyFileToUri(context, tempOutputArchiveFile, uri)
            tempOutputArchiveFile.delete()
        }

        return result
    }

    // Utils

    /**
     * Copy a file into a given Uri and returns 0 if ok (and -1 if not)
     */
    fun copyFileToUri(context: Context, file: File, uri: Uri): Int {

        /*    try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e:Exception) {
                Log.e(TAG,e.stackTraceToString())
                return -1
            }
        */
        try {
            file.inputStream().use { inputStream ->
                context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e:Exception) {
            Log.e(TAG,e.stackTraceToString())
            return -1
        }
        return 0
    }


    // Get a filename from an URI
    private fun getNameFromUriLastPathSegment(uri:Uri, defaultNameIfNull:String="emptyName"):String {
        val lastPathSegment = uri.lastPathSegment
        if (lastPathSegment != null) {
            val lastIndex = lastPathSegment.lastIndexOf('/')
            if (lastIndex>=0) {
                return lastPathSegment.substring(lastPathSegment.lastIndexOf('/')+1)
            }
            return defaultNameIfNull
        }
        return defaultNameIfNull
    }

}

