package fr.nourry.fnylib7z

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import fr.nourry.fnylib7z.databinding.ActivityMainBinding
import fr.nourry.fnylib7z.FnyLib7z.Companion.intFromJNIGetUriFileSize
import fr.nourry.fnylib7z.utils.FileUtils2
import fr.nourry.fnylib7z.utils.getUriListFromUri
import java.io.File

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"

        var PERMISSIONS = arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var rootTreeUri : Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.zipVersion.text = "7zip version : ${FnyLib7z.get7zVersionInfo()}"


        val uriInStorage = treeUriInSharedStorage()
        if (uriInStorage == null) {
            askTreeUriPermission()
        } else {
            val id = DocumentsContract.getTreeDocumentId(uriInStorage)
            val trueUriInStorage = DocumentsContract.buildDocumentUriUsingTree(uriInStorage, id)
            rootTreeUri = trueUriInStorage
            startApp()
        }
    }

    private fun startApp() {
        FnyLib7z.getInstance().initialize(this)

        val uriCache = Uri.fromFile(cacheDir)
        Log.v("Test JNI", "uri = $uriCache")
        // Test access fichiers
        val stdoutFilePath = cacheDir.absolutePath + File.separator + "stdout.txt"
        val stdoutFile = File(stdoutFilePath)
        val stderrFilePath = cacheDir.absolutePath + File.separator + "stderr.txt"
        val extractDir = File(cacheDir.absolutePath+ File.separator+"extract")
        val filtersList = listOf("*.jpg", "*.webp", "*.png", "*.gif", "*.bmp", "*.jpeg")
        val numPagesToExtract = listOf(0, 1, 7, 13, 16)
//        val numPagesToExtract = emptyList<Int>()

        var lastZipUri:Uri? = null
        var lastZipFile: File? = null
        var lastRarUri:Uri? = null
        var lastRarFile: File? = null

        var rapportTest = "TEST URI (with ACTION_OPEN_DOCUMENT_TREE) : \n"

        // Test access fichiers
        val comicUriList = getUriListFromUri(this, rootTreeUri, true)
        Log.v(TAG, "Exploring URIs in directory $rootTreeUri")
        for (comicUri in comicUriList) {
            Log.v(TAG, " - comicUri = $comicUri ")

            val path = FileUtils2(this).getPath(comicUri)
            if (path != null && path != "") {
                val file = File(path)
                val extension = file.extension.lowercase()

                if (extension == "zip" || extension == "cbz") {
                    lastZipFile = file
                    lastZipUri = comicUri
                } else if (extension == "rar" || extension == "cbr") {
                    lastRarFile = file
                    lastRarUri = comicUri
                }

                Log.v(TAG, "   path = $path exists=${file.exists()} size=${file.length()}")
//                val size = intFromJNIGetUriFileSize(path)
//                Log.v(TAG, "      size = $size")
            }
/*            val parcelFileDescriptor = contentResolver.openFileDescriptor(comicUri, "r")
            parcelFileDescriptor?.let {
                val i = parcelFileDescriptor.detachFd()
                val size2 = FnyLib7z.intFromJNIGetFDFileSize(i)
                Log.v(TAG, "    size2 = $size2")
                parcelFileDescriptor.close()
            }*/
        }

        // If a zip file was found, let's play with the last one
        if (lastZipFile != null && lastZipUri != null) {
            val fileComicToUnzip = lastZipFile

            if (fileComicToUnzip.exists()) {
                // Test lib7zip
                var result = 0
                val fileSize = fileComicToUnzip.length()

                // Extract files from Uri
                result = FnyLib7z.getInstance().uncompress(lastZipUri, dirToExtract=extractDir, numListToExtract=numPagesToExtract, filtersList=filtersList)
                rapportTest +=  "\nUnarchive ${lastZipFile.absolutePath} (fileSize=$fileSize) \n result=${FnyLib7z.getResultMessage(result)} in $extractDir"
                Log.v(TAG, " ZIP result = $result in $extractDir")

                // Create archive file from files
                val newArchiveFile = File(cacheDir.absolutePath+ File.separator+"newArchive.zip")
                result = FnyLib7z.getInstance().compressFiles(newArchiveFile, paths=listOf(extractDir.absolutePath+File.separator+"*.jpg"), stdErrPath=stderrFilePath, stdOutputPath=stdoutFilePath)
                rapportTest +=  "\n\ncompressFiles::  ${FnyLib7z.getResultMessage(result)} in $newArchiveFile"
                Log.v(TAG, "    compressFiles::  ${result==0} in $newArchiveFile")


                // Delete files in archive
                val filesToDeleteList = listOf("util.txt")
                result = FnyLib7z.getInstance().deleteInArchive(lastZipUri, filesToDeleteList)
                rapportTest +=  "\n\ndeleteInArchive:: ($filesToDeleteList) result=${FnyLib7z.getResultMessage(result)}"
                Log.v(TAG, "    deleteInArchive = ${result == 0} $filesToDeleteList")

                // List files with details
                result = FnyLib7z.getInstance().listFiles(lastZipUri, sortList=true, stdOutputPath=stdoutFilePath)
                rapportTest +=  "\n\nlistFilesWithUri:: result = ${FnyLib7z.getResultMessage(result)}"
                Log.v(TAG, "    listFilesWithUri:: result = $result")

                if (stdoutFile.exists()) {
                    val items = FnyLib7z.getInstance().parseListFile(stdoutFile)
                    var nbFiles = 0
                    var nbDirs= 0
                    items.forEach {
                        if (it.isDir)
                            nbDirs++
                        else
                            nbFiles++
                    }
                    rapportTest +=  "   nb files=$nbFiles     nb directories=$nbDirs"
                    Log.v(TAG, "   nb files=$nbFiles     nb directories=$nbDirs")

                }

            } else {
                rapportTest += "\n${lastZipFile.absolutePath} doesn't exist"
                Log.v(TAG, "the file doesn't exist !!")
            }
        } else {
            rapportTest += "\nlastZipFile is null !"
        }

        // If a rar file was found, let's play with the last one
/*        if (lastRarUri != null && lastRarFile != null) {
            if (lastRarFile.exists()) {
                val fileSize = lastRarFile.length()
                val result = Lib7z.getInstance().uncompress(lastRarUri, dirToExtract=extractDir, numListToExtract=numPagesToExtract, filtersList=filtersList)
                rapportTest +=  "\nUnarchive ${lastRarFile.absolutePath} (fileSize=$fileSize) \n result=${Lib7z.getResultMessage(result)} in $extractDir"
                Log.v(TAG, " RAR result = $result in $extractDir}")
            }
        }*/
        binding.sampleText.text = rapportTest
    }

    ///////////////// PERMISSION ACTION_OPEN_DOCUMENT_TREE

    // Return the first tree uri un the Shared Storage
    private fun treeUriInSharedStorage() : Uri? {
        for (perm in contentResolver.persistedUriPermissions) {
            if (DocumentsContract.isTreeUri(perm.uri)) {
                return perm.uri
            }
        }
        return null
    }

    private fun askTreeUriPermission() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            if (::rootTreeUri.isInitialized && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Optionally, specify a URI for the directory that should be opened in
                // the system file picker when it loads.
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, rootTreeUri)
            }
        }
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        permissionIntentLauncher.launch(intent)
    }

    private var permissionIntentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.e(TAG,"result=$result")
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let{ intent->

                var flags: Int = intent.flags
                flags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                intent.data?.let { treeUri ->
                    Log.e(TAG,"treeUri=$treeUri")
                    // treeUri is the Uri

                    val documentsTree = DocumentFile.fromTreeUri(this, treeUri)
                    documentsTree?.let {

                        // Check if it's a directory (should be...)
                        if (documentsTree.isDirectory) {
                            // Save this uri in PersistableUriPermission (keep only one, so delete the other ones)
                            releasePermissionsInSharedStorage()

                            contentResolver.takePersistableUriPermission(
                                treeUri,
                                flags
                            )

                            // Convert treeUri in a usable one
                            val id = DocumentsContract.getTreeDocumentId(treeUri)
                            val trueUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                            rootTreeUri = trueUri

                            startApp()
                        }
                    }
                }
            }
        } else {
            Log.w(TAG, "registerForActivityResult NOT OK !")
        }
    }

    private fun releasePermissionsInSharedStorage() {
        val perms = contentResolver.persistedUriPermissions
        for (perm in perms) {
            Log.e(TAG,"releaseOnePermission -> releasing ${perm.uri.path}}")
            contentResolver.releasePersistableUriPermission(perm.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            break
        }
    }
    // PERMISSION ACTION_OPEN_DOCUMENT_TREE
}