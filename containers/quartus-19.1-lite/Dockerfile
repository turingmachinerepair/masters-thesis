FROM 	fpga-base as install
ADD 	./quartus-lite-setup.tar /quartus	
RUN 	mkdir -p /opt/quartus && \
	/quartus/setup.sh --mode unattended --accept_eula 1 --installdir /opt/quartus
CMD 	["/opt/quartus/quartus/bin/quartus_cmd", "-v"]


FROM fpga-base 
COPY --from=install /opt/quartus /opt/quartus
CMD 	["/opt/quartus/quartus/bin/quartus_cmd", "-v"]
