# FnyLib7z

## Table of Contents

- **[Presentation](#presentation)**
- **[Usage](#usage)**
- **[Helper functions](#helper-functions)**
- **[Example](#example)**
- **[License](#license)**
- **[Author](#author)**

---

## Presentation

Android library based on 7-zip sources that allows you to open and compress files using path, File or Uri.

When I was writing [**myKomik**][1], I needed a way to open comics (archives) from an URI in order to be
able to use the `ACTION_OPEN_DOCUMENT_TREE` intent. Having found nothing, I decided to make my own library.
I started from [Hu Zong Yao's project][2] which integrates the [7z code][3] for Android, and I added
the possibility of manipulating archives no longer with their path, but with their FileDescriptor (by
modifying the 7-zip code and adding some parameters).

The current integrated 7-zip version is 16.04.

---

## Usage

* Add a new dependency in your build.gradle:

```
dependencies {
    ...
    implementation "io.github.frednourry:fnylib7z:0.9.1"
     or
    implementation ("io.github.frednourry:fnylib7z:0.9.1")
    ...
}
```


* There are two main functions, `get7zVersionInfo()` and `execute(...)`, but I have added some helper
  functions for specific uses (list, decompress, etc...). FnyLib7z is a singleton that need to be 
  initialized by `FnyLib7z.getInstance().initialize(context)`.

Example:
```
Log.v(TAG, "Current 7-zip version = ${FnyLib7z.get7zVersionInfo()}")
FnyLib7z.getInstance().initialize(this)     // This is your context
val my7zCommand = "e -aoa '/path/to/my/archive' '-o/dir/to/extract'"
val result = FnyLib7z.getInstance().execute(my7zCommand)
Log.v(TAG, "FnyLib7z execute $my7zCommand :: $result=(${FnyLib7z.getResultMessage(result)})")
```

With the exception of get7zVersionInfo(), on all functions, there are two optional parameters:
`stdOutputPath` and `stdErrPath` which gives you access to the execution logs (respectively the output
and the error messages).
Be sure to have the permission to create those files! (I highly recommend using the app's temporary
directory *cacheDir*).

Example:
```
val result = FnyLib7z.getInstance().execute("l /path/to/my/archive", stdOutputPath="/path/to/the/output/file", stdOutputPath="/path/to/the/error/file")
```

---

## Helper functions

There are four helper functions to list, decompress and compress or update archives. You can use as
input either a path, a File object, nUri (except for compress).

* #### List (and parsing) the content of an archive
To list the contents of an archive, you must first create the output file with `listFiles(...)`, and
then parse it with the `parseListFile(...)` method.
```
val filtersList = listOf("*.jpg", "*.gif")
val outputFile = "/path/to/the/output/file"
val result = FnyLib7z.getInstance().listFiles("/path/to/my/archive", sortList=true, filtersList=filtersList, stdOutputPath=outputFile)
if (result == FnyLib7z.RESULT_OK && stdoutFile.exists()) {
    val items = FnyLib7z.getInstance().parseListFile("/path/to/the/output/file")
    items.forEach {
        Log.v(TAG, "   ${it.name}: size=${it.size}")
    }
}
```

* #### Uncompress an archive
To extract the contents of an archive, use the `uncompress(...)` method. Be sure to have write permissions
on the output!
```
val filtersList = listOf("*.txt")       // Filter *.txt files
val numListToExtract = listOf(0, 1)     // Ask to only extract the first two files
val extractDir = /dir/to/extract        // Where to extract your files
val result = FnyLib7z.getInstance().uncompress("/path/to/my/archive", filtersList=filtersList, dirToExtract=extractDir, numListToExtract=numPagesToExtract)
Log.v(TAG, "FnyLib7z uncompress result=$result - ${FnyLib7z.getResultMessage(result)}")
```

* #### Add files in an archive (don't use Uri!)
The `compressFiles(...)` method adds the selected files to an archive (if the archive does not yet exist,
it will be created).
```
val newArchiveFile = File(cacheDir.absolutePath+ File.separator+"myNewArchive.zip")
val pathsToAddInArchive = listOf("/where/are/my/files/to/compress/*.jpg")
val result = FnyLib7z.getInstance().compressFiles(newArchiveFile, filtersList=pathsToAddInArchive, format="zip")
```

* #### Delete files in an archive
The `deleteInArchive(...)` method deletes the selected files in an existing archive.
```
val filesToDeleteList = listOf("util.txt")
val result = FnyLib7z.getInstance().deleteInArchive("/path/to/my/archive", filtersList=filesToDeleteList, caseSensitive=true)
```

---

## Example
 You will find a complete example in [MainActivity.kt][5]. Use the `ACTION_OPEN_DOCUMENT_TREE` intent 
   to select a directory with ZIP files and the application will test on the last zip file in the directory, using its Uri.

 #### A reminder for each method and their parameters
| Method            | Path | File | Uri | filtersList | caseSensitive| sortList | numListToExtract
| :-                | :-:  |  :-: | :-: | :-:         | :-:          | :-:      | :-:              
| `listFiles`       | ✔    | ✔   |  ✔  | ✔           | ✖            | ✔       | ✖               
| `uncompress`      | ✔    | ✔   |  ✔  | ✔           | ✔            | ✔       | ✔                
| `compressFiles`   | ✔    | ✔   |  ✖  | ✔           | ✔            | ✖       | ✖                
| `deleteInArchive` | ✔    | ✔   |  ✔  | ✔           | ✔            | ✖       | ✖

 Note: When using *sortList* and *numListToExtract* together with `uncompress`, the list of items is first sorted, then filtered (in that order)

---

## License

Because I use the source code of 7-zip, the license is the same (ie mostly GNU LGPL license):
see https://www.7-zip.org/
```
7-Zip is free software with open source. The most of the code is under the GNU LGPL license. Some parts of the code are under the BSD 3-clause License. Also there is unRAR license restriction for some parts of the code. Read 7-Zip License information.
```

---

## Author

Frederic Nourry - [@frednourry][4] on GitHub


[1]: https://github.com/frednourry/myKomik
[2]: https://github.com/hzy3774/AndroidP7zip
[3]: https://www.7-zip.org/
[4]: https://github.com/frednourry
[5]: https://github.com/frednourry/FnyLib7z/blob/main/app/src/main/java/fr/nourry/fnylib7z/MainActivity.kt
