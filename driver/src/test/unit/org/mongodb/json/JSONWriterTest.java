/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.json;

import org.bson.BSONInvalidOperationException;
import org.bson.types.BSONTimestamp;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.bson.types.RegularExpression;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class JSONWriterTest {
    private StringWriter stringWriter;
    private JSONWriter writer;

    @Before
    public void before() {
        stringWriter = new StringWriter();
        writer = new JSONWriter(stringWriter, new JSONWriterSettings());
    }

    private static class TestData<T> {
        private final T value;
        private final String expected;

        public TestData(final T value, final String expected) {
            this.value = value;
            this.expected = expected;
        }
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowExceptionForBooleanWhenWritingBeforeStartingDocument() {
        writer.writeBoolean("b1", true);
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowExceptionForNameWhenWritingBeforeStartingDocument() {
        writer.writeName("name");
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowExceptionForStringWhenStateIsValue() {
        writer.writeStartDocument();
        writer.writeString("SomeString");
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowExceptionWhenEndingAnArrayWhenStateIsValue() {
        writer.writeStartDocument();
        writer.writeEndArray();
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowExceptionWhenWritingASecondName() {
        writer.writeStartDocument();
        writer.writeName("f1");
        writer.writeName("i2");
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowExceptionWhenEndingADocumentBeforeValueIsWritten() {
        writer.writeStartDocument();
        writer.writeName("f1");
        writer.writeEndDocument();
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowAnExceptionWhenTryingToWriteASecondValue() {
        writer.writeDouble(100);
        writer.writeString("i2");
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowAnExceptionWhenTryingToWriteJavaScript() {
        writer.writeDouble(100);
        writer.writeJavaScript("var i");
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowAnExceptionWhenWritingANameInAnArray() {
        writer.writeStartDocument();
        writer.writeStartArray("f2");
        writer.writeName("i3");
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowAnExceptionWhenEndingDocumentInTheMiddleOfWritingAnArray() {
        writer.writeStartDocument();
        writer.writeStartArray("f2");
        writer.writeEndDocument();
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowAnExceptionWhenEndingAnArrayInASubDocument() {
        writer.writeStartDocument();
        writer.writeStartArray("f2");
        writer.writeStartDocument();
        writer.writeEndArray();
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowAnExceptionWhenWritingANameInAnArrayEvenWhenSubDocumentExistsInArray() {
        writer.writeStartDocument();
        writer.writeStartArray("f2");
        writer.writeStartDocument();
        writer.writeEndDocument();
        writer.writeName("i3");
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowAnExceptionWhenAttemptingToEndAnArrayThatWasNotStarted() {
        writer.writeStartDocument();
        writer.writeStartArray("f2");
        writer.writeEndArray();
        writer.writeEndArray();
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowAnErrorIfTryingToWriteNameIntoAJavascriptScope() {
        writer.writeStartDocument();
        writer.writeJavaScriptWithScope("js1", "var i = 1");

        writer.writeName("b1");
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowAnErrorIfTryingToWriteValueIntoAJavascriptScope() {
        writer.writeStartDocument();
        writer.writeJavaScriptWithScope("js1", "var i = 1");

        writer.writeBinaryData(new Binary(new byte[]{0, 0, 1, 0}));
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowAnErrorIfTryingToWriteArrayIntoAJavascriptScope() {
        writer.writeStartDocument();
        writer.writeJavaScriptWithScope("js1", "var i = 1");

        writer.writeStartArray();
    }

    @Test(expected = BSONInvalidOperationException.class)
    public void shouldThrowAnErrorIfTryingToWriteEndDocumentIntoAJavascriptScope() {
        writer.writeStartDocument();
        writer.writeJavaScriptWithScope("js1", "var i = 1");

        writer.writeEndDocument();
    }

    @Test
    public void testEmptyDocument() {
        writer.writeStartDocument();
        writer.writeEndDocument();
        String expected = "{ }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testSingleString() {
        writer.writeStartDocument();
        writer.writeString("abc", "xyz");
        writer.writeEndDocument();
        String expected = "{ \"abc\" : \"xyz\" }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testIndentedEmptyDocument() {
        writer = new JSONWriter(stringWriter, new JSONWriterSettings(true));
        writer.writeStartDocument();
        writer.writeEndDocument();
        String expected = "{ }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testIndentedOneElement() {
        writer = new JSONWriter(stringWriter, new JSONWriterSettings(true));
        writer.writeStartDocument();
        writer.writeString("name", "value");
        writer.writeEndDocument();
        String expected = String.format("{%n  \"name\" : \"value\"%n}");
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testIndentedTwoElements() {
        writer = new JSONWriter(stringWriter, new JSONWriterSettings(true));
        writer.writeStartDocument();
        writer.writeString("a", "x");
        writer.writeString("b", "y");
        writer.writeEndDocument();
        String expected = String.format("{%n  \"a\" : \"x\",%n  \"b\" : \"y\"%n}");
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testDouble() {
        List<TestData<Double>> tests = asList(new TestData<Double>(0.0, "0.0"), new TestData<Double>(0.0005, "5.0E-4"),
                                              new TestData<Double>(0.5, "0.5"), new TestData<Double>(1.0, "1.0"),
                                              new TestData<Double>(1.5, "1.5"), new TestData<Double>(1.5E+40, "1.5E40"),
                                              new TestData<Double>(1.5E-40, "1.5E-40"),
                                              new TestData<Double>(1234567890.1234568E+123, "1.2345678901234568E132"),
                                              new TestData<Double>(Double.MAX_VALUE, "1.7976931348623157E308"),
                                              new TestData<Double>(Double.MIN_VALUE, "4.9E-324"),

                                              new TestData<Double>(-0.0005, "-5.0E-4"),
                                              new TestData<Double>(-0.5, "-0.5"),
                                              new TestData<Double>(-1.0, "-1.0"),
                                              new TestData<Double>(-1.5, "-1.5"),
                                              new TestData<Double>(-1.5E+40, "-1.5E40"),
                                              new TestData<Double>(-1.5E-40, "-1.5E-40"),
                                              new TestData<Double>(-1234567890.1234568E+123, "-1.2345678901234568E132"),

                                              new TestData<Double>(Double.NaN, "NaN"),
                                              new TestData<Double>(Double.NEGATIVE_INFINITY, "-Infinity"),
                                              new TestData<Double>(Double.POSITIVE_INFINITY, "Infinity"));
        for (final TestData<Double> cur : tests) {
            stringWriter = new StringWriter();
            writer = new JSONWriter(stringWriter, new JSONWriterSettings());
            writer.writeStartDocument();
            writer.writeDouble("d", cur.value);
            writer.writeEndDocument();
            String expected = "{ \"d\" : " + cur.expected + " }";
            assertEquals(expected, stringWriter.toString());
        }
    }

    @Test
    public void testInt64Shell() {
        List<TestData<Long>> tests = asList(new TestData<Long>(Long.MIN_VALUE, "NumberLong(\"-9223372036854775808\")"),
                                            new TestData<Long>(Integer.MIN_VALUE - 1L, "NumberLong(\"-2147483649\")"),
                                            new TestData<Long>(Integer.MIN_VALUE + 0L, "NumberLong(-2147483648)")
                                               ,
                                            new TestData<Long>(0L, "NumberLong(0)"),
                                            new TestData<Long>(Integer.MAX_VALUE + 0L, "NumberLong(2147483647)"),
                                            new TestData<Long>(Integer.MAX_VALUE + 1L, "NumberLong(\"2147483648\")"),
                                            new TestData<Long>(Long.MAX_VALUE, "NumberLong(\"9223372036854775807\")"));
        for (final TestData<Long> cur : tests) {
            stringWriter = new StringWriter();
            writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.Shell));
            writer.writeStartDocument();
            writer.writeInt64("l", cur.value);
            writer.writeEndDocument();
            String expected = "{ \"l\" : " + cur.expected + " }";
            assertEquals(expected, stringWriter.toString());
        }
    }

    @Test
    public void testInt64Strict() {
        List<TestData<Long>> tests = asList(new TestData<Long>(Long.MIN_VALUE, "-9223372036854775808"),
                                            new TestData<Long>(Integer.MIN_VALUE - 1L, "-2147483649"),
                                            new TestData<Long>(Integer.MIN_VALUE - 0L, "-2147483648"),
                                            new TestData<Long>(0L, "0"),
                                            new TestData<Long>(Integer.MAX_VALUE + 0L, "2147483647"),
                                            new TestData<Long>(Integer.MAX_VALUE + 1L, "2147483648"),
                                            new TestData<Long>(Long.MAX_VALUE, "9223372036854775807"));

        for (final TestData<Long> cur : tests) {
            stringWriter = new StringWriter();
            writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.Strict));
            writer.writeStartDocument();
            writer.writeInt64("l", cur.value);
            writer.writeEndDocument();
            String expected = "{ \"l\" : " + cur.expected + " }";
            assertEquals(expected, stringWriter.toString());
        }
    }

    @Test
    public void testEmbeddedDocument() {
        writer.writeStartDocument();
        writer.writeStartDocument("doc");
        writer.writeInt32("a", 1);
        writer.writeInt32("b", 2);
        writer.writeEndDocument();
        writer.writeEndDocument();
        String expected = "{ \"doc\" : { \"a\" : 1, \"b\" : 2 } }";
        assertEquals(expected, stringWriter.toString());
    }


    @Test
    public void testIndentedEmbeddedDocument() {
        writer = new JSONWriter(stringWriter, new JSONWriterSettings(true));
        writer.writeStartDocument();
        writer.writeStartDocument("doc");
        writer.writeInt32("a", 1);
        writer.writeInt32("b", 2);
        writer.writeEndDocument();
        writer.writeEndDocument();
        String expected = String.format("{%n  \"doc\" : {%n    \"a\" : 1,%n    \"b\" : 2%n  }%n}");
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testArray() {
        writer.writeStartDocument();
        writer.writeStartArray("array");
        writer.writeInt32(1);
        writer.writeInt32(2);
        writer.writeInt32(3);
        writer.writeEndArray();
        writer.writeEndDocument();
        String expected = "{ \"array\" : [1, 2, 3] }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testBinaryStrict() {
        List<TestData<Binary>> tests = asList(new TestData<Binary>(new Binary(new byte[0]),
                                                                   "{ \"$binary\" : \"\", "
                                                                   + "\"$type\" : \"0\" }"),
                                              new TestData<Binary>(new Binary(new byte[]{1}),
                                                                   "{ \"$binary\" : \"AQ==\", "
                                                                   + "\"$type\" : \"0\" }"),
                                              new TestData<Binary>(new Binary(new byte[]{1, 2}),
                                                                   "{ \"$binary\" : \"AQI=\", "
                                                                   + "\"$type\" : \"0\" }"),
                                              new TestData<Binary>(new Binary(new byte[]{1, 2, 3}),
                                                                   "{ \"$binary\" : \"AQID\", "
                                                                   + "\"$type\" : \"0\" }"),
                                              new TestData<Binary>(new Binary((byte) 0x80, new byte[]{1, 2, 3}),
                                                                   "{ \"$binary\" : \"AQID\", "
                                                                   + "\"$type\" : \"80\" }"));
        for (final TestData<Binary> cur : tests) {
            stringWriter = new StringWriter();
            writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.Strict));
            writer.writeStartDocument();
            writer.writeBinaryData("binary", cur.value);
            writer.writeEndDocument();
            String expected = "{ \"binary\" : " + cur.expected + " }";
            assertEquals(expected, stringWriter.toString());
        }
    }

    @Test
    public void testBinaryShell() {
        List<TestData<Binary>> tests = asList(new TestData<Binary>(new Binary(new byte[0]), "new BinData(0, \"\")"),
                                              new TestData<Binary>(new Binary(new byte[]{1}), "new BinData(0, \"AQ==\")"),
                                              new TestData<Binary>(new Binary(new byte[]{1, 2}), "new BinData(0, \"AQI=\")"),
                                              new TestData<Binary>(new Binary(new byte[]{1, 2, 3}), "new BinData(0, \"AQID\")"),
                                              new TestData<Binary>(new Binary((byte) 0x80, new byte[]{1, 2, 3}),
                                                                   "new BinData(128, \"AQID\")"));
        for (final TestData<Binary> cur : tests) {
            stringWriter = new StringWriter();
            writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.Shell));
            writer.writeStartDocument();
            writer.writeBinaryData("binary", cur.value);
            writer.writeEndDocument();
            String expected = "{ \"binary\" : " + cur.expected + " }";
            assertEquals(expected, stringWriter.toString());
        }
    }

    @Test
    public void testDateTimeStrict() {
        List<TestData<Date>> tests = asList(new TestData<Date>(new Date(0), "{ \"$date\" : 0 }"),
                                            new TestData<Date>(new Date(Long.MAX_VALUE), "{ \"$date\" : 9223372036854775807 }"),
                                            new TestData<Date>(new Date(Long.MIN_VALUE), "{ \"$date\" : -9223372036854775808 }"));
        for (final TestData<Date> cur : tests) {
            stringWriter = new StringWriter();
            writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.Strict));
            writer.writeStartDocument();
            writer.writeDateTime("date", cur.value.getTime());
            writer.writeEndDocument();
            String expected = "{ \"date\" : " + cur.expected + " }";
            assertEquals(expected, stringWriter.toString());
        }
    }

    @Test
    public void testDateTimeShell() {
        List<TestData<Date>> tests = asList(new TestData<Date>(new Date(0), "ISODate(\"1970-01-01T00:00:00.000Z\")"),
                                            new TestData<Date>(new Date(1), "ISODate(\"1970-01-01T00:00:00.001Z\")"),
                                            new TestData<Date>(new Date(-1), "ISODate(\"1969-12-31T23:59:59.999Z\")"),
                                            new TestData<Date>(new Date(Long.MAX_VALUE), "new Date(9223372036854775807)"),
                                            new TestData<Date>(new Date(Long.MIN_VALUE), "new Date(-9223372036854775808)"));
        for (final TestData<Date> cur : tests) {
            stringWriter = new StringWriter();
            writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.Shell));
            writer.writeStartDocument();
            writer.writeDateTime("date", cur.value.getTime());
            writer.writeEndDocument();
            String expected = "{ \"date\" : " + cur.expected + " }";
            assertEquals(expected, stringWriter.toString());
        }
    }


    @Test
    public void testDateTimeTenGen() {
        List<TestData<Date>> tests = asList(new TestData<Date>(new Date(0), "new Date(0)"),
                                            new TestData<Date>(new Date(Long.MAX_VALUE), "new Date(9223372036854775807)"),
                                            new TestData<Date>(new Date(Long.MIN_VALUE), "new Date(-9223372036854775808)"));
        for (final TestData<Date> cur : tests) {
            stringWriter = new StringWriter();
            writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.TenGen));
            writer.writeStartDocument();
            writer.writeDateTime("date", cur.value.getTime());
            writer.writeEndDocument();
            String expected = "{ \"date\" : " + cur.expected + " }";
            assertEquals(expected, stringWriter.toString());
        }
    }

    @Test
    public void testJavaScript() {
        writer.writeStartDocument();
        writer.writeJavaScript("f", "function f() { return 1; }");
        writer.writeEndDocument();
        String expected = "{ \"f\" : { \"$code\" : \"function f() { return 1; }\" } }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testJavaScriptWithScope() {
        writer.writeStartDocument();
        writer.writeJavaScriptWithScope("f", "function f() { return n; }");
        writer.writeStartDocument();
        writer.writeInt32("n", 1);
        writer.writeEndDocument();
        writer.writeEndDocument();
        String expected =
            "{ \"f\" : { \"$code\" : \"function f() { return n; }\", " + "\"$scope\" : { \"n\" : 1 } } }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testMaxKey() {
        writer.writeStartDocument();
        writer.writeMaxKey("maxkey");
        writer.writeEndDocument();
        String expected = "{ \"maxkey\" : { \"$maxKey\" : 1 } }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testMinKey() {
        writer.writeStartDocument();
        writer.writeMinKey("minkey");
        writer.writeEndDocument();
        String expected = "{ \"minkey\" : { \"$minKey\" : 1 } }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testNull() {
        writer.writeStartDocument();
        writer.writeNull("null");
        writer.writeEndDocument();
        String expected = "{ \"null\" : null }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testObjectIdShell() {
        writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.Shell));
        ObjectId objectId = new ObjectId("4d0ce088e447ad08b4721a37");

        writer.writeStartDocument();
        writer.writeObjectId("_id", objectId);
        writer.writeEndDocument();

        String expected = "{ \"_id\" : ObjectId(\"4d0ce088e447ad08b4721a37\") }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testObjectIdStrict() {
        ObjectId objectId = new ObjectId("4d0ce088e447ad08b4721a37");

        writer.writeStartDocument();
        writer.writeObjectId("_id", objectId);
        writer.writeEndDocument();

        String expected = "{ \"_id\" : { \"$oid\" : \"4d0ce088e447ad08b4721a37\" } }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testRegularExpressionShell() {
        List<TestData<RegularExpression>> tests;
        tests = asList(new TestData<RegularExpression>(new RegularExpression(""), "/(?:)/"),
                       new TestData<RegularExpression>(new RegularExpression("a"), "/a/"),
                       new TestData<RegularExpression>(new RegularExpression("a/b"), "/a\\/b/"),
                       new TestData<RegularExpression>(new RegularExpression("a\\b"), "/a\\b/"),
                       new TestData<RegularExpression>(new RegularExpression("a", "i"), "/a/i"),
                       new TestData<RegularExpression>(new RegularExpression("a", "m"), "/a/m"),
                       new TestData<RegularExpression>(new RegularExpression("a", "x"), "/a/x"),
                       new TestData<RegularExpression>(new RegularExpression("a", "s"), "/a/s"),
                       new TestData<RegularExpression>(new RegularExpression("a", "imxs"), "/a/imxs"));
        for (final TestData<RegularExpression> cur : tests) {
            stringWriter = new StringWriter();
            writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.Shell));
            writer.writeStartDocument();
            writer.writeRegularExpression("regex", cur.value);
            writer.writeEndDocument();
            String expected = "{ \"regex\" : " + cur.expected + " }";
            assertEquals(expected, stringWriter.toString());
        }
    }

    @Test
    public void testRegularExpressionStrict() {
        List<TestData<RegularExpression>> tests;
        tests = asList(new TestData<RegularExpression>(new RegularExpression(""), "{ \"$regex\" : \"\", "
                                                                                  + "\"$options\" : \"\" "
                                                                                  + "}"),
                       new TestData<RegularExpression>(new RegularExpression("a"), "{ \"$regex\" : \"a\","
                                                                                   + " \"$options\" : \"\" "
                                                                                   + "}"),
                       new TestData<RegularExpression>(new RegularExpression("a/b"), "{ \"$regex\" : "
                                                                                     + "\"a/b\", "
                                                                                     + "\"$options\" : \"\" "
                                                                                     + "}"),
                       new TestData<RegularExpression>(new RegularExpression("a\\b"), "{ \"$regex\" : "
                                                                                      + "\"a\\\\b\", "
                                                                                      + "\"$options\" : \"\" "
                                                                                      + "}"),
                       new TestData<RegularExpression>(new RegularExpression("a", "i"), "{ \"$regex\" : \"a\","
                                                                                        + " \"$options\" : \"i\""
                                                                                        + " }"),
                       new TestData<RegularExpression>(new RegularExpression("a", "m"), "{ \"$regex\" : \"a\","
                                                                                        + " \"$options\" : \"m\""
                                                                                        + " }"),
                       new TestData<RegularExpression>(new RegularExpression("a", "x"), "{ \"$regex\" : \"a\","
                                                                                        + " \"$options\" : \"x\""
                                                                                        + " }"),
                       new TestData<RegularExpression>(new RegularExpression("a", "s"), "{ \"$regex\" : \"a\","
                                                                                        + " \"$options\" : \"s\""
                                                                                        + " }"),
                       new TestData<RegularExpression>(new RegularExpression("a", "imxs"),
                                                       "{ \"$regex\" : \"a\"," + " \"$options\" : \"imxs\" }"));
        for (final TestData<RegularExpression> cur : tests) {
            stringWriter = new StringWriter();
            writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.Strict));
            writer.writeStartDocument();
            writer.writeRegularExpression("regex", cur.value);
            writer.writeEndDocument();
            String expected = "{ \"regex\" : " + cur.expected + " }";
            assertEquals(expected, stringWriter.toString());
        }
    }

    @Test
    public void testString() {
        List<TestData<String>> tests;
        tests = asList(new TestData<String>("", "\"\""), new TestData<String>(" ", "\" \""),
                       new TestData<String>("a", "\"a\""), new TestData<String>("ab", "\"ab\""),
                       new TestData<String>("abc", "\"abc\""),
                       new TestData<String>("abc\u0000def", "\"abc\\u0000def\""),
                       new TestData<String>("\\", "\"\\\\\""), new TestData<String>("\'", "\"'\""),
                       new TestData<String>("\"", "\"\\\"\""), new TestData<String>("\0", "\"\\u0000\""),
                       new TestData<String>("\b", "\"\\b\""), new TestData<String>("\f", "\"\\f\""),
                       new TestData<String>("\n", "\"\\n\""), new TestData<String>("\r", "\"\\r\""),
                       new TestData<String>("\t", "\"\\t\""), new TestData<String>("\u0080", "\"\\u0080\""),
                       new TestData<String>("\u0080\u0081", "\"\\u0080\\u0081\""),
                       new TestData<String>("\u0080\u0081\u0082", "\"\\u0080\\u0081\\u0082\""));
        for (final TestData<String> cur : tests) {
            stringWriter = new StringWriter();
            writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.Strict));
            writer.writeStartDocument();
            writer.writeString("str", cur.value);
            writer.writeEndDocument();
            String expected = "{ \"str\" : " + cur.expected + " }";
            assertEquals(expected, stringWriter.toString());
        }
    }

    @Test
    public void testSymbol() {
        writer.writeStartDocument();
        writer.writeSymbol("symbol", "name");
        writer.writeEndDocument();
        String expected = "{ \"symbol\" : { \"$symbol\" : \"name\" } }";
        assertEquals(expected, stringWriter.toString());
    }

    //
    @Test
    public void testTimestampStrict() {
        writer.writeStartDocument();
        writer.writeTimestamp("timestamp", new BSONTimestamp(1000, 1));
        writer.writeEndDocument();
        String expected = "{ \"timestamp\" : { \"$timestamp\" : { \"t\" : 1000, \"i\" : 1 } } }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testTimestampShell() {
        writer = new JSONWriter(stringWriter, new JSONWriterSettings(JSONMode.Shell));
        writer.writeStartDocument();
        writer.writeTimestamp("timestamp", new BSONTimestamp(1000, 1));
        writer.writeEndDocument();
        String expected = "{ \"timestamp\" : Timestamp(1000, 1) }";
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testUndefined() {
        writer.writeStartDocument();
        writer.writeUndefined("undefined");
        writer.writeEndDocument();
        String expected = "{ \"undefined\" : undefined }";
        assertEquals(expected, stringWriter.toString());
    }
}