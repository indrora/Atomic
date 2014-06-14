
[![Build Status](https://travis-ci.org/indrora/Atomic.svg?branch=master)](https://travis-ci.org/indrora/Atomic) [![Crowdin](https://d322cqt584bo4o.cloudfront.net/atomic/localized.png)](https://crowdin.net/project/atomic)

Atomic - a Fork of Yaaic
========================

Atomic scratches an itch I (@indrora) had: A sexy IRC client that worked.
After trying HoloIRC, AndChat, yaaic, and several others, yaaic came the
closest and after pressure from friends, I decided to go ahead and
fork yaaic.

I'm changing the name to prevent clashes with yaaic, and to preserve
Sebastian's honour. 

The last bit of the original yaaic readme is kept below;

Newer icons used within are from the Android core icon pack, from Google.
Ostensibly, they are (C) Google, inc. They are provided free of charge
to Android developers. I claim no exclusive usage of them other than to say
I use them.

The application icon is derived from the CC-BA 3.0 Unported icon by Designmodo. Its details can be seen at Iconfinder:

* https://www.iconfinder.com/icons/115722/chemistry_lab_science_test_vials_icon#size=512

Things that I have changed/added
================================

* Icons now match the Holo style (predominantly)
* Highlight notification sound can be chosen from the settings

Many of the translations are in flux. Please [contribute on Crowdin](http://www.crowdin.net/project/atomic) to make sure
your language is spoken by Atomic!


Current/future features/changes
===============================

* Option to not hide the soft keyboard on SEND (Somewhat DONE!)
* Changing the size of the font in dp not in px (Working on it!)
* Update of all the icons (DONE!)
* Selecting a message copies it to the clipboard (There's support in there somewhere)
* Option to not scroll when a new message appears in a channel (Kinda done!)

Building Atomic
===============

You will need to generate a keystore for Atomic:

     keytool -genkey -keystore application/keystore -alias <alias>

and go and add the following to application/ant.properties

    key.store=keystore
    key.alias=<alias>
    key.store.password=<password>
    key.alias.password=<password>

run

    cd application
    android update project -p . -n Atomic -s --target android-19
    ant installd

Your connected device will now get a debug signed 

Fork Details
============

Atomic is based off of yaaic: Yet Another Android IRC Client.

View the original project on GitHub:

 http://github.com/pocmo/Yaaic

Check if there' a bug there, too!

License credits
===============


Atomic includes the PircBot IRC API written by Paul Mutton available
under the GNU General Public License (GPL). http://www.jibble.org

Atomic makes use of the following libraries:

* [ViewPagerIndicator](http://viewpagerindicator.com), licensed under the Apache license, version 2.0
* [ActionBarSherlock](http://actionbarsherlock.com), licensed under the Apache license, version 2.0
* [MemorizingTrustManager](https://github.com/ge0rg/MemorizingTrustManager), licensed under the Apache license, version 2.0

The Yaaic icon was designed by http://www.androidicons.com

License (GPLv3)
===============

    Copyright 2009-2013 Sebastian Kaspari
    Copyright 2014-     Morgan `Indrora' Gangwere

    Yaaic/Atomic is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    
    Yaaic/Atomic is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with this software. If not, see http://www.gnu.org/licenses/

