/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
 Copyright (c) 2001 Lapo Luchini.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright 
 notice, this list of conditions and the following disclaimer in 
 the documentation and/or other materials provided with the distribution.

 3. The names of the authors may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHORS
 OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.jcraft.jzlib;

import java.io.*;

public class ZOutputStream extends OutputStream {

	protected ZStream z = new ZStream();
	protected int bufsize = 512;
	protected int flush = JZlib.Z_NO_FLUSH;
	protected byte[] buf = new byte[this.bufsize], buf1 = new byte[1];
	protected boolean compress;

	protected OutputStream out;

	public ZOutputStream(OutputStream out) {
		super();
		this.out = out;
		this.z.inflateInit();
		this.compress = false;
	}

	public ZOutputStream(OutputStream out, int level) {
		this(out, level, false);
	}

	public ZOutputStream(OutputStream out, int level, boolean nowrap) {
		super();
		this.out = out;
		this.z.deflateInit(level, nowrap);
		this.compress = true;
	}

	@Override
	public void write(int b) throws IOException {
		this.buf1[0] = (byte) b;
		write(this.buf1, 0, 1);
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException {
		if (len == 0)
			return;
		int err;
		this.z.next_in = b;
		this.z.next_in_index = off;
		this.z.avail_in = len;
		do {
			this.z.next_out = this.buf;
			this.z.next_out_index = 0;
			this.z.avail_out = this.bufsize;
			if (this.compress)
				err = this.z.deflate(this.flush);
			else
				err = this.z.inflate(this.flush);
			if (err != JZlib.Z_OK)
				throw new ZStreamException((this.compress ? "de" : "in") + "flating: " + this.z.msg);
			this.out.write(this.buf, 0, this.bufsize - this.z.avail_out);
		} while (this.z.avail_in > 0 || this.z.avail_out == 0);
	}

	public int getFlushMode() {
		return (this.flush);
	}

	public void setFlushMode(int flush) {
		this.flush = flush;
	}

	public void finish() throws IOException {
		int err;
		do {
			this.z.next_out = this.buf;
			this.z.next_out_index = 0;
			this.z.avail_out = this.bufsize;
			if (this.compress) {
				err = this.z.deflate(JZlib.Z_FINISH);
			} else {
				err = this.z.inflate(JZlib.Z_FINISH);
			}
			if (err != JZlib.Z_STREAM_END && err != JZlib.Z_OK)
				throw new ZStreamException((this.compress ? "de" : "in") + "flating: " + this.z.msg);
			if (this.bufsize - this.z.avail_out > 0) {
				this.out.write(this.buf, 0, this.bufsize - this.z.avail_out);
			}
		} while (this.z.avail_in > 0 || this.z.avail_out == 0);
		flush();
	}

	public void end() {
		if (this.z == null)
			return;
		if (this.compress) {
			this.z.deflateEnd();
		} else {
			this.z.inflateEnd();
		}
		this.z.free();
		this.z = null;
	}

	@Override
	public void close() throws IOException {
		try {
			try {
				finish();
			} catch (IOException ignored) {
				// TODO: naughty, should at least print error
			}
		} finally {
			end();
			this.out.close();
			this.out = null;
		}
	}

	/**
	 * Returns the total number of bytes input so far.
	 */
	public long getTotalIn() {
		return this.z.total_in;
	}

	/**
	 * Returns the total number of bytes output so far.
	 */
	public long getTotalOut() {
		return this.z.total_out;
	}

	@Override
	public void flush() throws IOException {
		this.out.flush();
	}

}
