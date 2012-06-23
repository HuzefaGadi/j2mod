//License
/***
 * Java Modbus Library (jamod)
 * Copyright (c) 2002-2004, jamod development team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the author nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ***/
package com.ghgande.j2mod.modbus.cmd;

import java.net.InetAddress;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.ModbusUDPTransaction;
import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.net.UDPMasterConnection;

/**
 * Class that implements a simple commandline tool for writing to a digital
 * output.
 * <p>
 * Note that if you write to a remote I/O with a Modbus protocol stack, it will
 * most likely expect that the communication is <i>kept alive</i> after the
 * first write message.<br>
 * This can be achieved either by sending any kind of message, or by repeating
 * the write message within a given period of time.<br>
 * If the time period is exceeded, then the device might react by turning pos
 * all signals of the I/O modules. After this timeout, the device might require
 * a reset message.
 * 
 * @author Dieter Wimberger
 * @version 1.2rc1 (09/11/2004)
 */
public class UDPDOTest {

	public static void main(String[] args) {

		UDPMasterConnection conn = null;
		ModbusUDPTransaction trans = null;
		WriteCoilRequest req = null;

		InetAddress addr = null;

		int ref = 0;
		boolean set = false;
		int repeat = 1;
		int port = Modbus.DEFAULT_PORT;

		try {

			// 1. Setup the parameters
			if (args.length < 3) {
				printUsage();
				System.exit(1);
			} else {
				try {
					String astr = args[0];
					int idx = astr.indexOf(':');
					if (idx > 0) {
						port = Integer.parseInt(astr.substring(idx + 1));
						astr = astr.substring(0, idx);
					}
					addr = InetAddress.getByName(astr);
					ref = Integer.parseInt(args[1]);
					set = "true".equals(args[2]);

					if (args.length == 4) {
						repeat = Integer.parseInt(args[3]);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					printUsage();
					System.exit(1);
				}
			}

			// 2. Open the connection
			conn = new UDPMasterConnection(addr);
			conn.setPort(port);
			conn.connect();

			// 3. Prepare a request
			req = new WriteCoilRequest(ref, set);
			req.setUnitID(0);
			if (Modbus.debug)
				System.out.println("Request: " + req.getHexMessage());

			// 4. Prepare the transaction
			trans = new ModbusUDPTransaction(conn);
			trans.setRequest(req);

			// 5. Execute the transaction repeat times
			int k = 0;
			do {
				trans.execute();

				if (Modbus.debug)
					System.out.println("Response: "
							+ trans.getResponse().getHexMessage());
				k++;
			} while (k < repeat);

			// 6. Close the connection
			conn.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void printUsage() {
		System.out.println(
				"java com.ghgande.j2mod.modbus.cmd.UDPDOTest <address{:<port>} [String]> <register [int16]> <state [boolean]> {<repeat [int]>}");
	}
}
