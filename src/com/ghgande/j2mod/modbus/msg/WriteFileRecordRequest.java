//License
/***
 * Java Modbus Library (jamod)
 * Copyright 2010-2012, greenHouse Gas and Electric
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
package com.ghgande.j2mod.modbus.msg;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.msg.ReadFileRecordRequest.RecordRequest;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;


/**
 * Class implementing a <tt>Write File Record</tt> request.
 * 
 * @author Julie Haugh (jfh@ghgande.com)
 * @version @version@ (@date@)
 */
public final class WriteFileRecordRequest extends ModbusRequest {
	private int m_ByteCount;
	private RecordRequest[] m_Records;
	
	public class RecordRequest {
		private int m_FileNumber;
		private int m_RecordNumber;
		private int m_WordCount;
		private	byte	m_Data[];

		public int getFileNumber() {
			return m_FileNumber;
		}

		public int getRecordNumber() {
			return m_RecordNumber;
		}

		public int getWordCount() {
			return m_WordCount;
		}

		public SimpleRegister getRegister(int register) {
			if (register < 0 || register >= m_WordCount) {
				throw new IndexOutOfBoundsException("0 <= " +
						register + " < " + m_WordCount);
			}
			byte b1 = m_Data[register * 2];
			byte b2 = m_Data[register * 2 + 1];
			
			SimpleRegister result = new SimpleRegister(b1, b2);
			return result;
		}

		/**
		 * getRequestSize -- return the size of the response in bytes.
		 */
		public int getRequestSize() {
			return 7 + m_WordCount * 2;
		}

		public void getRequest(byte[] request, int offset) {
			request[offset++] = 6;
			request[offset++] = (byte) (m_FileNumber >> 8);
			request[offset++] = (byte) (m_FileNumber & 0xFF);
			request[offset++] = (byte) (m_RecordNumber >> 8);
			request[offset++] = (byte) (m_RecordNumber & 0xFF);
			request[offset++] = (byte) (m_WordCount >> 8);
			request[offset++] = (byte) (m_WordCount & 0xFF);
			
			System.arraycopy(m_Data, 0, request, offset, m_Data.length);
		}

		public byte[] getRequest() {
			byte[] request = new byte[7 + 2 * m_WordCount];

			getRequest(request, 0);

			return request;
		}

		public RecordRequest(int file, int record, short[] values) {
			m_FileNumber = file;
			m_RecordNumber = record;
			m_WordCount = values.length;
			m_Data = new byte[m_WordCount * 2];

			int offset = 0;
			for (int i = 0; i < m_WordCount; i++) {
				m_Data[offset++] = (byte) (values[i] >> 8);
				m_Data[offset++] = (byte) (values[i] & 0xFF);
			}
		}
	}


	/**
	 * getRequestSize -- return the total request size.  This is useful
	 * for determining if a new record can be added.
	 * 
	 * @returns size in bytes of response.
	 */
	public int getRequestSize() {
		if (m_Records == null)
			return 1;
		
		int size = 1;
		for (int i = 0;i < m_Records.length;i++)
			size += m_Records[i].getRequestSize();
		
		return size;
	}
	
	/**
	 * getRequestCount -- return the number of record requests in this
	 * message.
	 */
	public int getRequestCount() {
		if (m_Records == null)
			return 0;
		
		return m_Records.length;
	}
	
	/**
	 * getRecord -- return the record request indicated by the reference
	 */
	public RecordRequest getRecord(int index) {
		return m_Records[index];
	}
	
	/**
	 * addRequest -- add a new record request.
	 */
	public void addRequest(RecordRequest request) {
		if (request.getRequestSize() + getRequestSize() > 248)
			throw new IllegalArgumentException();
		
		if (m_Records == null)
			m_Records = new RecordRequest[1];
		else {
			RecordRequest old[] = m_Records;
			m_Records = new RecordRequest[old.length + 1];
			
			System.arraycopy(old, 0, m_Records, 0, old.length);
		}
		m_Records[m_Records.length - 1] = request;
		
		setDataLength(getRequestSize());
	}

	/**
	 * createResponse -- create an empty response for this request.
	 */
	public ModbusResponse getResponse() {
		ReportSlaveIDResponse response = null;

		response = new ReportSlaveIDResponse();

		/*
		 * Copy any header data from the request.
		 */
		response.setHeadless(isHeadless());
		if (! isHeadless()) {
			response.setTransactionID(getTransactionID());
			response.setProtocolID(getProtocolID());
		}
		
		/*
		 * Copy the unit ID and function code.
		 */
		response.setUnitID(getUnitID());
		response.setFunctionCode(getFunctionCode());

		return response;
	}
	
	/**
	 * The ModbusCoupler doesn't have a means of writing file records.
	 */
	public ModbusResponse createResponse() {
		throw new RuntimeException();
	}

	/**
	 * writeData -- output this Modbus message to dout.
	 */
	public void writeData(DataOutput dout) throws IOException {
		dout.write(getMessage());
	}

	/**
	 * readData -- dummy function.  There is no data with the request.
	 */
	public void readData(DataInput din) throws IOException {
		m_ByteCount = din.readUnsignedByte();

		int recordCount = m_ByteCount / 7;
		m_Records = new RecordRequest[recordCount];

		for (int i = 0; i < recordCount; i++) {
			if (din.readByte() != 6)
				throw new IOException();

			int file = din.readUnsignedShort();
			int record = din.readUnsignedShort();
			
			if (record < 0 || record >= 10000)
				throw new IOException();

			int count = din.readUnsignedShort();
			short registers[] = new short[count];
			for (int j = 0;j < count;j++) {
				registers[j] = din.readShort();
			}
			m_Records[i] = new RecordRequest(file, record, registers);
		}
	}

	/**
	 * getMessage -- return the raw binary message.
	 */
	public byte[] getMessage() {
		byte	results[] = new byte[getRequestSize()];

		results[0] = (byte) (getRequestSize() - 1);
		
		int offset = 1;
		for (int i = 0;i < m_Records.length;i++) {
			m_Records[i].getRequest(results, offset);
			offset += m_Records[i].getRequestSize();
		}
		return results;
	}

	/**
	 * Constructs a new <tt>Write File Record</tt> request
	 * instance.
	 */
	public WriteFileRecordRequest() {
		super();
		
		setFunctionCode(Modbus.WRITE_FILE_RECORD);
		
		/*
		 * Set up space for the initial header.
		 */
		setDataLength(1);
	}
}