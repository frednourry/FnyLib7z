package fr.nourry.fnylib7z.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import java.io.*
import java.net.URLDecoder
import java.util.*

val comicExtensionList = listOf("cbr", "cbz", "pdf", "rar", "zip", "cb7", "7z")

fun concatPath(path1:String, path2:String):String {
    return path1+File.separator+path2
}

fun isFileExists(path: String):Boolean {
    val f = File(path)
    return f.exists()
}

// Return true if and only if the extension is 'jpg', 'gif', 'png', 'jpeg', 'webp' or 'bmp'
fun isImageExtension(extension:String) : Boolean {
    return (extension == "jpg") || (extension == "gif") || (extension == "png") || (extension == "jpeg") || (extension == "webp") || (extension == "bmp")
}

// Return true if the given file path is from an image
fun isFilePathAnImage(filename:String) : Boolean {
    val ext = File(filename).extension.lowercase()
    return isImageExtension(ext)
}


// Delete files in a directory
fun clearFilesInDir(dir:File) {
    Log.v("FileSystem","clearFilesInDir(${dir.absoluteFile})")
    if (dir.exists() && dir.isDirectory) {
        val list = dir.listFiles()
        if (list != null) {
            for (file in list) {
                if (file.isFile) {
                    file.delete()
                }
            }
        }
    }
}

// Delete a file (a simple file or a directory)
fun deleteFile(f:File): Boolean {
    Log.v("FileSystem","deleteFile(${f.absoluteFile})")
    if (f.exists()) {
        try {
            if (f.isDirectory) {
                // List all the files in the directory and delete them
                val list = f.listFiles()
                for (file in list!!) {
                    deleteFile(file)
                }
            }
            return f.delete()
        } catch (e: SecurityException) {
            Log.e("FileSystem","Error while deleting a file")
            e.printStackTrace()
        }
    }
    return true
}


// Create a directory if it's not exists
fun createDirectory(path:String) {
    val dir = File(path)
    if (dir.exists()) {
        Log.v("FileSystem","createDirectory:: $path already exists")
        return
    } else {
        if (dir.mkdirs()) {
            Log.v("FileSystem","createDirectory:: $path created")
        } else {
            Log.w("FileSystem","createDirectory:: $path :: error while creating")
        }
    }
}

fun getSizeInMo(size:Long): Float {
    return size.toFloat()/1048576f     // NOTE: 1048576 = 1024 x 1024
}

fun getLocalDirName(rootTreeUri:Uri?, currentUri:Uri?):String {
    Log.d("FileSystem","getLocalDirName rootTreeUri=$rootTreeUri currentUri=$currentUri")
    if (currentUri != null && rootTreeUri != null) {
        val rootLastSegment = rootTreeUri.lastPathSegment
        val currentLastSegment = currentUri.lastPathSegment

        if (rootLastSegment != null && currentLastSegment != null) {
            val lastSlash = rootLastSegment.lastIndexOf('/')
            return currentLastSegment.substring(lastSlash)
        }
    }
    return "--"
}



// Get the last modification date of a file
fun getReadableDate(l:Long): String {
    val date = Date(l)
    return date.toString()
}

// Return the extension of a filename
fun getExtension(filename:String): String {
    val lastPointPos = filename.lastIndexOf('.')
    return if (lastPointPos != -1 && filename.length > lastPointPos+1)
        filename.substring(lastPointPos+1).lowercase()
    else
        ""
}

fun stripExtension(filename:String): String = filename.substring(0, filename.lastIndexOf('.'))


// Retrieves a list of comics uri order by its type and name
// Precond: the given uri is a directory
fun getUriListFromUri(context: Context, uri:Uri, bOnlyFile:Boolean = false): List<Uri> {
    Log.v("FileSystem","getUriListFromUri uri = $uri")

    val docId = DocumentsContract.getDocumentId(uri)
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, docId)
    var results: List<Uri> = emptyList()
    val resultDirs: MutableList<Uri> = mutableListOf()
    val resultComics: MutableList<Uri> = mutableListOf()

    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED
    )

    var c: Cursor? = null
    try {
        c = context.applicationContext.contentResolver.query(childrenUri, projection, null, null, null)
        if (c != null) {
            // Get the URIs
            while (c.moveToNext()) {
                val documentId: String = c.getString(0)
                var name0 = c.getString(1)
                name0 = name0.replace("%(?![0-9a-fA-F]{2})".toRegex(), "%25")
                name0 = name0.replace("\\+".toRegex(), "%2B")
                name0 = name0.replace("!", "\\!")
                var name = URLDecoder.decode(name0, "utf-8")
                val mime = c.getString(2)
                val size = c.getString(3)
                val lastModified = c.getString(4)

                val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
//                Log.v("FileSystem","getUriListFromUri :: documentUri = $documentUri")

                if (!bOnlyFile && DocumentsContract.Document.MIME_TYPE_DIR == mime) {
                    resultDirs.add(documentUri)
                } else {
                    // Filter by file type
                    val extension = getExtension(name)
                    if (extension in comicExtensionList) {
                        resultComics.add(documentUri)
                    }
                }
            }

            // Order each list
            val tempResultDirs = resultDirs.sortedBy { it.path }
            val tempResultComics = resultComics.sortedBy { it.path }

            // Concat
            results = tempResultDirs.plus(tempResultComics)
        }
    } catch (e: java.lang.Exception) {
        Log.w("FileSystem","getUriListFromUri :: Failed query: $e")
    } finally {
        c?.close()
    }
    return results
}

// Get the parent uri (if any) by looking "%2F"
fun getParentUriPath(uri:Uri):String {
    val path = uri.toString()
    val i = path.lastIndexOf("%2F")
    return if (i>0) {
        path.substring(0, i)
    } else
        ""
}

// Get a temporary file that doesn't exist
// tempDirectory should exist !
fun getTempFile(tempDirectory:File, name:String, checkExist:Boolean):File {        // "current" = same temp dir than the ComicLoadingManager
    var tempFile =  concatPath(tempDirectory.absolutePath, "$name.tmp")
    var cpt = 0
    var file = File(tempFile)
    if (!checkExist)
        return file

    while (file.exists()) {
        cpt++
        tempFile =  concatPath(tempDirectory.absolutePath, "$name-$cpt.tmp")
        file = File(tempFile)
    }
    return file
}

/**
 * Copy a file from its Uri into a given file and returns the file if ok
 */
fun copyFileFromUri(context:Context, uri: Uri, file:File): File? {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    } catch (e:Exception) {
        Log.e("FileSystem",e.stackTraceToString())
        return null
    }
    return file
}

@Throws(IOException::class)
fun File.copyTo(file: File) {
    inputStream().use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}


fun getFilesList(dir: File): List<File> {
    Log.d("FileSystem", "getFilesList:: ${dir.absolutePath}")
    val l = dir.listFiles()

    if (l != null) {
        val temp = l.sortedWith(compareBy{it.name}).filter { f-> (f.isFile) }
        Log.d("FileSystem", temp.toString())
        return temp
    }
    return emptyList()
}
fun getDirectoriesList(dir: File): List<File> {
    Log.d("FileSystem", "getDirectoriesList:: ${dir.absolutePath}")
    val l = dir.listFiles()

    if (l != null) {
        val temp = l.sortedWith(compareBy{it.name}).filter { f-> (f.isDirectory) }
        Log.d("FileSystem", temp.toString())
        return temp
    }
    return emptyList()
}