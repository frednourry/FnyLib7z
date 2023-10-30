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
import fr.nourry.fnylib7z.utils.FileUtils2
import fr.nourry.fnylib7z.utils.getUriListFromUri
import io.github.frednourry.FnyLib7z
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

        var rapportTest = "Test URI access (given ACTION_OPEN_DOCUMENT_TREE) with FnyLib7z: \n"

        // Test file access and
        val fileUriList = getUriListFromUri(this, rootTreeUri, true)
        Log.v(TAG, "Exploring URIs in directory $rootTreeUri")
        val fileUtil2 = FileUtils2(this)        // Used to convert an Uri into a path - only for this demo purpose...
        for (uri in fileUriList) {
            Log.v(TAG, " - uri = $uri ")

            val path = fileUtil2.getPath(uri)
            if (path != null && path != "") {
                val file = File(path)
                val extension = file.extension.lowercase()

                if (extension == "zip" || extension == "cbz") {
                    lastZipFile = file
                    lastZipUri = uri
                }

                Log.v(TAG, "   path = $path size=${file.length()}")
            }
        }

        // If a zip file was found, let's play with the last one
        if (lastZipFile != null && lastZipUri != null) {
            val fileToUnzip = lastZipFile

            if (fileToUnzip.exists()) {
                var result = 0
                val fileSize = fileToUnzip.length()

                // Extract files from Uri
                result = FnyLib7z.getInstance().uncompress(lastZipUri, dirToExtract=extractDir, numListToExtract=numPagesToExtract, filtersList=filtersList)
                rapportTest +=  "\nUnarchive ${lastZipFile.absolutePath} (fileSize=$fileSize) \n result=${FnyLib7z.getResultMessage(result)} in $extractDir"
                Log.v(TAG, "  uncompress:: result = $result in $extractDir")

                // List files with details
                result = FnyLib7z.getInstance().listFiles(lastZipUri, sortList=true, stdOutputPath=stdoutFilePath)
                rapportTest +=  "\n\nlistFiles:: result = ${FnyLib7z.getResultMessage(result)}"
                Log.v(TAG, "  listFiles:: result = $result")

                if (result == FnyLib7z.RESULT_OK && stdoutFile.exists()) {
                    val items = FnyLib7z.getInstance().parseListFile(stdoutFile)
                    var nbFiles = 0
                    var nbDirs= 0
                    items.forEach {
                        if (it.isDir)
                            nbDirs++
                        else
                            nbFiles++
                    }
                    rapportTest +=  "    nb files=$nbFiles     nb directories=$nbDirs"
                    Log.v(TAG, "    nb files=$nbFiles     nb directories=$nbDirs")
                }

                // Create archive file with jpg files in temp directory
                val newArchiveFile = File(cacheDir.absolutePath+ File.separator+"newArchive.zip")
                result = FnyLib7z.getInstance().compressFiles(newArchiveFile, filtersList=listOf(extractDir.absolutePath+File.separator+"*.jpg"))
                rapportTest +=  "\n\ncreate/update an archive:: result = ${FnyLib7z.getResultMessage(result)} in ${newArchiveFile.absolutePath}"
                Log.v(TAG, "  compressFiles::  ${result==0} in $newArchiveFile")

                // Delete files in archive
                val filesToDeleteList = listOf("util.txt")
//                val filesToDeleteList = listOf("*.txt")
                result = FnyLib7z.getInstance().deleteInArchive(lastZipUri, filtersList=filesToDeleteList, stdOutputPath=stdoutFilePath, stdErrPath = stderrFilePath)
                rapportTest +=  "\n\ndeleteInArchive:: ($filesToDeleteList) result=${FnyLib7z.getResultMessage(result)}"
                Log.v(TAG, "  deleteInArchive = ${result == 0} $filesToDeleteList")

            } else {
                rapportTest += "\n${lastZipFile.absolutePath} doesn't exist"
                Log.v(TAG, "the file doesn't exist !!")
            }
        } else {
            rapportTest += "\nlastZipFile is null !"
            Log.v(TAG, "lastZipFile is null !")
        }

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