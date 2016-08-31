package java.testbench;

import static java.nio.charset.CodingErrorAction.IGNORE;
import static java.nio.charset.CodingErrorAction.REPLACE;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.util.stream.Collectors;

import org.junit.Test;

/**
 * Examples using CharsetEncoder to continue usage of the inferior ISO-8859-1 instead of UTF-8 by either
 * - Using the default action - converting unstorable characters to ?
 * - Choose a custom replacement character
 * - Ignore the unstorable characters
 * - Convert the unstorable characters into U+<hexvalue>, ie "\u2260" becomes "U+2260"  
 */
public class UsingISO88591WithCharsetEncoderTest {

	@Test
	public void replace_ISO_unstorable_with_custom_char() throws Exception {

		assertThat(encodeToISO88591("x ≠ y", '!')).isEqualTo("x ! y");
	}

	public static String encodeToISO88591(String string, char replacement) throws CharacterCodingException {
		CharsetEncoder encoder = ISO_8859_1.newEncoder() //
				.replaceWith(("" + replacement).getBytes()) //
				.onMalformedInput(REPLACE) //
				.onUnmappableCharacter(REPLACE);
		ByteBuffer bytes = encoder.encode(CharBuffer.wrap(string.toCharArray()));
		return new String(bytes.array(), UTF_8);
	}

	@Test
	public void ignore_ISO_unstorable() throws Exception {

		assertThat(encodeToISO88591Ignore("x ≠ y")).isEqualTo("x  y");
		assertThat(encodeToISO88591Ignore("x ≠ y≠")).isEqualTo("x  y");
		assertThat(encodeToISO88591Ignore("≠x ≠≠ y≠")).isEqualTo("x  y");
	}

	public static String encodeToISO88591Ignore(String string) throws CharacterCodingException {
		CharsetEncoder encoder = ISO_8859_1.newEncoder().onMalformedInput(IGNORE).onUnmappableCharacter(IGNORE);
		ByteBuffer bytes = encoder.encode(CharBuffer.wrap(string.toCharArray()));
		return new String(bytes.array(), 0, bytes.limit());
	}

	@Test
	public void default_replacement_is_questionmark() throws Exception {
		String isoConverted = new String("x ≠ y".getBytes(ISO_8859_1), ISO_8859_1);
		assertThat(isoConverted).isEqualTo("x ? y");
	}

	@Test
	public void fails_using_ISO_to_store_Unicode() throws Exception {
		String mathematicalExpression = "x ≠ y";
		byte[] encoded = mathematicalExpression.getBytes(ISO_8859_1);
		String decoded = new String(encoded, ISO_8859_1);

		assertThat(decoded.equals(mathematicalExpression)).describedAs("Fails since ISO-8859-1 cannot store ≠")
				.isFalse();
	}

	@Test
	public void each_codepoint_of_ISO() throws Exception {
		assertThat("x ≠ y".codePoints().toArray()).containsSequence(120, 32, 0x2260, 32, 121);
	}

	@Test
	public void each_byte_of_ISO() throws Exception {
		assertThat("x ≠ y".getBytes(ISO_8859_1)).containsSequence(toByte(120, 32, 63, 32, 121));
	}

	private byte[] toByte(int... bytes) {
		byte[] result = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			result[i] = (byte) bytes[i];
		}
		return result;
	}

	@Test
	public void getting_bytes_from_char() {
		char c = 'a';
		int first = ((c & 0xFF00) >> 8);
		int second = (c & 0x00FF);
		assertThat(first).isEqualTo(0);
		assertThat(second).isEqualTo(97);
	}

	@Test
	public void replace_ISO_unstorable_with_unicode_rep() throws Exception {

		assertThat(encodeToISO88591("x \u2260 y")).isEqualTo("x U+2260 y");
		assertThat(encodeToISO88591("? \u2260 y")).isEqualTo("? U+2260 y");
	}

	// Not optimized!
	public static String encodeToISO88591(String string) throws CharacterCodingException {
		return string.codePoints().mapToObj(codepoint -> {
			if (isoCanStore(codepoint))
				return new String(Character.toChars(codepoint));
			return "U+" + Integer.toHexString(codepoint);
		}).collect(Collectors.joining());
	}

	private final static byte[] QUESTION_BYTES = "?".getBytes();
	private static boolean isoCanStore(int codepoint) {
		if (codepoint == '?')
			return true;
		ByteBuffer encoded = ISO_8859_1.encode(CharBuffer.wrap(Character.toChars(codepoint)));
		return encoded.compareTo(ByteBuffer.wrap(QUESTION_BYTES)) != 0;
	}
}
