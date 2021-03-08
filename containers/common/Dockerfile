FROM centos:8
RUN dnf install -y epel-release && dnf update -y && dnf install -y \
gtk2{,.i686} unixODBC{,.i686} libxml2{,.i686} libXt.i686 libXtst.i686 libXdmcp.i686 libpng12.i686 libnsl \
dejavu-{sans,sans-mono,serif}-fonts 'liberation-*-fonts' \
xorg-x11-xauth xterm openmotif firefox \
&& dnf clean all && rm -rf /var/cache/yum \
&& ln -sf usr/lib64/libXm.so.4.0.3 /usr/lib64/libXm.so.3 && ldconfig 

#xorg-x11-fonts \
