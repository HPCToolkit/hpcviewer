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
# help display
#------------------------------------------------------------


usage()
{
    cat <<EOF
Usage:
  hpcviewer [viewer-options] [database-directory]

Options:
  -h, --help               Print help.

  -jh, --java-heap <size>  Set the JVM maximum heap size. The value of <size> must be
                           in m (megabytes) or g (gigabytes). Example:
                              hpcviewer  -jh 3g
                           will use a JVM maximum heap size of 3GB for this execution
                           of hpcviewer.
EOF
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
# Look for arguments
#------------------------------------------------------------

viewer_args=""
PARAMS=""
while (( "$#" )); do
  case "$1" in 
    -h|--help)
      usage
      exit 0
      ;;
    -jh|--java-heap)
      if [ -n "$2" ] && [ ${2:0:1} != "-" ]; then
        viewer_args="-vmargs -Xmx$2"
        shift 2
      else
        echo "Error: Argument for $1 is missing" >&2
        exit 1
      fi
      ;;
    *) # preserve positional arguments
      PARAMS="$PARAMS $1"
      shift
      ;;
  esac
done
# set positional arguments in their proper place
eval set -- "$PARAMS"

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
    cmd="$viewer -data $workspace -configuration $workspace $PARAMS  $viewer_args  " 
    exec $cmd 2> $stderr
else
    warn "HOME is not set, proceeding anyway"
    exec "$viewer" "$@"
fi
