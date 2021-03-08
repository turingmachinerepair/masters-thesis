FROM 	fpga-base as install
ADD 	./quartus-18-setup.run /quartus-18-setup.run	
RUN 	mkdir -p /opt/quartus && \
	/quartus-18-setup.run --mode unattended --accept_eula 1 --installdir /opt/quartus
CMD 	["/opt/quartus/quartus/bin/quartus_cmd", "-v"]

