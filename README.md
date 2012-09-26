# Android PageKite App #

This is an Android PageKite app.


## What is PageKite? ##

PageKite is a protocol for dynamic, tunneled reverse proxying of arbitrary
TCP byte streams.  It is particularly well suited for making a HTTP server
on a device without a public IP address visible to the wider Internet, but
can also be used for a variety of other things, including SSH access.

For more information about PageKite, see: <http://pagekite.org/>


## What is in the box? ##

This is a basic Android app which allows the user to assign a single DNS
name (kite name) to his phone and expose HTTP, HTTPS, Websocket and SSH
servers to the public web.


## Getting started ##

The app should be available for [download from Google
Play](https://play.google.com/store/apps/details?id=net.pagekite.app) or
[directly from pagekite.net](https://pagekite.net/pk/android/PageKiteApp.apk).

When first run the app will prompt the user to sign up or log in to their
[pagekite.net](https://pagekite.net/) account, but this dialog can be
dismissed and the app should work just fine with personal frontend relays as
well.

The app has in-line help, which can also be [viewed
online](http://htmlpreview.github.com/?https://github.com/pagekite/AndroidPageKiteApp/blob/master/assets/help/about.html).


## Hacking Howto ##

You will need both the Android SDK *and* the Android NDK to build PageKite.

PageKite for Android depends on
[libpagekite](https://github.com/pagekite/libpagekite),
which in turn depends on [this version of
OpenSSL](https://github.com/guardianproject/openssl-android).

The following steps should suffice to check out all the code you need and
configure the tree for building:

    mkdir PageKiteApp
    cd PageKiteApp
    git clone https://github.com/pagekite/libpagekite.git
    # ...
    git clone https://github.com/pagekite/AndroidPageKiteApp.git
    # ...
    cd AndroidPageKiteApp
    rm -f jni
    ln -s ../libpagekite jni
    cd ../libpagekite
    git clone https://github.com/guardianproject/openssl-android.git
    # ...

To build libpagekite:

    cd PageKiteApp
    cd AndroidPageKiteApp
    export NDK_PROJECT_PATH=/path/to/android-ndk
    make -f jni/Makefile android
    # ...

(If the build fails, please consult the
[libpagekite](https://github.com/pagekite/libpagekite) documentation for
hints or details missing from this page.)

Once this is all done, you should be able to import the AndroidPageKiteApp
into Eclipse and work with the code like any other Android app.

**Note:** If someone would like to contribute ant build rules to make this
all more CLI-friendly, that would be most appreciated!


## License and Copyright ##

PageKite for Android is Copyright 2012, The Beanstalks Project ehf.

This code is released under the GNU Affero General Public License version 3,
but may also be used (under specific conditions) according to the terms of the
GNU Lesser General Public License.  Please see the file COPYING.md for details
on which license applies to you.

Commercial support for this code, as well as managed front-end relay service,
are available from <https://pagekite.net/>.

Development of this code was partially sponsored by
[SURFnet](http://www.surfnet.nl) and the [Icelandic Technology Development
fund](http://www.rannis.is/).
