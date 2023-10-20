FnyLib7z
==========

Presentation
------------

Android library based on 7-zip sources that allows you to open and compress files using paths or URIs.

When I was writing [**myKomik**][1], I needed a way to open comics (archives) from its URI and to use the **ACTION_OPEN_DOCUMENT_TREE** Intent. Having found nothing satisfactory, I decided to make my own.
I started from [Hu Zong Yao's project][2] which integrates the [7z code][3] for Android, and I added the possibility of manipulating archives no longer with their path, but with their FileDescriptor.

The current 7-zip version is 16.04.

Licence
-------
Same as 7-zip : https://www.7-zip.org/

Author
------
Frederic Nourry - [@frednourry][4] on GitHub

-----------------------------------

[1]: https://github.com/frednourry/myKomik
[2]: https://github.com/hzy3774/AndroidP7zip
[3]: https://www.7-zip.org/
[4]: https://github.com/frednourry
