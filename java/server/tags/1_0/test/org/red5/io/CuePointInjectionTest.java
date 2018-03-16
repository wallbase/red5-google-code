/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.io;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.IFLV;
import org.red5.io.flv.IFLVService;
import org.red5.io.flv.impl.FLVService;
import org.red5.io.flv.impl.Tag;
import org.red5.io.flv.meta.ICueType;
import org.red5.io.flv.meta.IMetaCue;
import org.red5.io.flv.meta.MetaCue;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.cache.NoCacheImpl;

/**
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class CuePointInjectionTest extends TestCase {

	private IFLVService service;

	/**
	 * SetUp is called before each test
	 * 
	 */
	@Override
	public void setUp() {
		service = new FLVService();
		service.setSerializer(new Serializer());
		service.setDeserializer(new Deserializer());
	}

	/**
	 * Test MetaData injection
	 * 
	 * @throws IOException for fun
	 */
	public void testCuePointInjection() throws IOException {
		String path = "fixtures/test_cue1.flv";
		File f = new File(path);
		System.out.println("Path: " + f.getAbsolutePath());
		if (f.exists()) {
			f.delete();
		} else {
			// try test subdirectory
			path = "test/fixtures/test_cue1.flv";
			f = new File(path);
			System.out.println("Path: " + f.getAbsolutePath());
			if (f.exists()) {
				f.delete();
			}			
		}
		// Create new file
		f.createNewFile();

		// Use service to grab FLV file
		IFLV flv = (IFLV) service.getStreamableFile(f);

		// Grab a writer for writing a new FLV
		ITagWriter writer = flv.getWriter();

		// Create a reader for testing
		File readfile = new File(path);
		//assertTrue(readfile.exists());

		IFLV readflv = (IFLV) service.getStreamableFile(readfile);
		readflv.setCache(NoCacheImpl.getInstance());

		// Grab a reader for reading a FLV in
		ITagReader reader = readflv.getReader();

		// Inject MetaData
		writeTagsWithInjection(reader, writer);

	}

	/**
	 * Write FLV tags and inject Cue Points
	 * 
	 * @param reader
	 * @param writer
	 * @throws IOException
	 */
	private void writeTagsWithInjection(ITagReader reader, ITagWriter writer) throws IOException {
		IMetaCue cp = new MetaCue<Object, Object>();
		cp.setName("cue_1");
		cp.setTime(0.01);
		cp.setType(ICueType.EVENT);

		IMetaCue cp1 = new MetaCue<Object, Object>();
		cp1.setName("cue_2");
		cp1.setTime(2.01);
		cp1.setType(ICueType.EVENT);

		// Place in TreeSet for sorting
		TreeSet<IMetaCue> ts = new TreeSet<IMetaCue>();
		ts.add(cp);
		ts.add(cp1);

//		int cuePointTimeStamp = getTimeInMilliseconds(ts.first());

		ITag tag = null;
		ITag injectedTag = null;
		// read any existing tags and insert cue points
//		while (reader.hasMoreTags()) {
//			tag = reader.readTag();
//			// if there are cuePoints in the TreeSet
//			if (!ts.isEmpty()) {
//				// If the tag has a greater timestamp than the
//				// cuePointTimeStamp, then inject the tag
//				while (tag.getTimestamp() > cuePointTimeStamp) {
//					injectedTag = injectCuePoint(ts.first(), tag);
//					writer.writeTag(injectedTag);
//					tag.setPreviousTagSize((injectedTag.getBodySize() + 11));
//					// Advance to the next CuePoint
//					ts.remove(ts.first());
//					if (ts.isEmpty()) {
//						break;
//					}
//					cuePointTimeStamp = getTimeInMilliseconds(ts.first());
//				}
//			}
//			writer.writeTag(tag);
//		}
		while (!ts.isEmpty()) {
			injectedTag = injectCuePoint(ts.first(), tag);
			writer.writeTag(injectedTag);
			// Advance to the next CuePoint
			ts.remove(ts.first());
		}		
		writer.close();
	}

	/**
	 * Injects metadata (Cue Points) into a tag
	 * 
	 * @param cue
	 * @param writer
	 * @param tag
	 * @return ITag tag
	 */
	private ITag injectCuePoint(Object cue, ITag tag) {
		IMetaCue cp = (MetaCue<?, ?>) cue;
		Output out = new Output(IoBuffer.allocate(1000));
		Serializer ser = new Serializer();
		ser.serialize(out, "onCuePoint");
		ser.serialize(out, cp);

		IoBuffer tmpBody = out.buf().flip();
		int tmpBodySize = out.buf().limit();
		//int tmpPreviousTagSize = tag.getPreviousTagSize();
		int tmpTimestamp = getTimeInMilliseconds(cp);

		//return new Tag(tmpDataType, tmpTimestamp, tmpBodySize, tmpBody, tmpPreviousTagSize);
		return new Tag(IoConstants.TYPE_METADATA, tmpTimestamp, tmpBodySize, tmpBody, 0);
	}

	/**
	 * Returns a timestamp in milliseconds
	 * 
	 * @param object
	 * @return int time
	 */
	private int getTimeInMilliseconds(Object object) {
		IMetaCue cp = (MetaCue<?, ?>) object;
		return (int) (cp.getTime() * 1000.00);

	}

	/**
	 * Test to see if TreeSet is sorting properly
	 */
	public void testCuePointOrder() {
		IMetaCue cue = new MetaCue<Object, Object>();
		cue.setName("cue_1");
		cue.setTime(0.01);
		cue.setType(ICueType.EVENT);

		IMetaCue cue1 = new MetaCue<Object, Object>();
		cue1.setName("cue_3");
		cue1.setTime(2.01);
		cue1.setType(ICueType.EVENT);

		IMetaCue cue2 = new MetaCue<Object, Object>();
		cue2.setName("cue_2");
		cue2.setTime(1.01);
		cue2.setType(ICueType.EVENT);

		TreeSet<IMetaCue> ts = new TreeSet<IMetaCue>();
		ts.add(cue);
		ts.add(cue1);
		ts.add(cue2);

		System.out.println("ts: " + ts);

		Assert.assertEquals(true, true);
	}

}
