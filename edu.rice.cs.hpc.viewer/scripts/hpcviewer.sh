#!/bin/bash
#
# Copyright (c) 2002-2019, Rice University.
# See the file README.License for details.
#
# Launch the viewer binary and set the workspace directory.
# This script is only for Unix and X windows.
#
# $Id$
#

name=hpcviewer
arch=`uname -p`
workspace="${HOME}/.hpctoolkit/${name}/${arch}"

# Substitute the Java bindir from the install script, if needed.
# JAVA_BINDIR=/path/to/java/bindir

if test -d "$JAVA_BINDIR" ; then
    PATH="${JAVA_BINDIR}:$PATH"
fi

#------------------------------------------------------------
# Error messages
#------------------------------------------------------------

die()
{
    echo "$0: error: $*" 1>&2
    exit 1
}

warn()
{
    echo "$0: warning: $*" 1>&2
}

#------------------------------------------------------------
# Find the hpctoolkit directory.
#------------------------------------------------------------

script="$0"
if test -L "$script" ; then
    script=`readlink "$script"`
fi
bindir=`dirname "$script"`
bindir=`( cd "$bindir" && pwd )`
viewer="${bindir}/../libexec/${name}/${name}"
test -x "$viewer"  || die "executable $viewer not found"
test -n "$DISPLAY" || die "DISPLAY variable is not set"

#------------------------------------------------------------
# Check the java version.
#------------------------------------------------------------

java_version=`java -version 2>&1 | grep -i vers | head -1`
if test "x$java_version" = x ; then
    die "unable to find program 'java' on your PATH"
fi
minor=`expr "$java_version" : '[^.]*\.\([0-9]*\)'`
test "$minor" -ge 7 >/dev/null 2>&1
#if test $? -ne 0 ; then
#    echo "$java_version is too old, use Java 1.7 or later"
#fi

java_vendor=`java -version | sed -n 2p  | awk '{print $1}'`
if test "$java_vendor" = "gij"; then
    echo "GNU JVM (gij) is not supported. Please use JVM from Oracle/SUN or IBM"
fi

#------------------------------------------------------------
# Prepare the environment.
#------------------------------------------------------------

# UBUNTU's unity menu is broken and only displays hpcviewer's
# file menu. Address this by disabling UBUNTU's unity menus.
# This setting is harmless on non-UBUNTU platforms.
export UBUNTU_MENUPROXY=0

# Eclipse Mars has many issues with GTK3, used by default
# to go back to GTK2, we need to set variable environment before
# running the viewer. This variable seems harmless for other 
# applications (except SWT-based programs)
# see bugs on 
#  https://bugs.eclipse.org/bugs/show_bug.cgi?id=470994
#  https://bugs.eclipse.org/bugs/show_bug.cgi?id=478962
#  https://bugs.eclipse.org/bugs/show_bug.cgi?id=470994
export SWT_GTK3=0

#------------------------------------------------------------
# Launch the viewer.
#------------------------------------------------------------

if test -d "$HOME" ; then
	stderr="$workspace"/hpcviewer.log
	mkdir -p "$workspace"
	echo "Redirect standard error to $stderr"
    exec "$viewer" -data "$workspace" -configuration "$workspace" "$@" 2> $stderr
else
    warn "HOME is not set, proceeding anyway"
    exec "$viewer" "$@"
fi
