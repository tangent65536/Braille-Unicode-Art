package tangent65536.braille;

/**
 * Copyright (c) 2020, Tangent65536.
 *  All rights reserved.
 * 
 * A utility class for handling Braille characters.
 * 
 * 6-cell bit order (ISO 11548-1):
 *  1 4
 *  2 5
 *  3 6
 *  7 8
 * 
 * Custom 8-cell bit order used as the index:
 *  1 5
 *  2 6
 *  3 7
 *  4 8
 */
public final class BrailleCharacters
{
	/**
	 * Quick access.
	 */
	private static final Pattern[] BRALLIES = new Pattern[256];
	
	/**
	 * Quick access for required bitwise operations.
	 */
	private static final short[] BOOL_INDEX_TO_DOTS = new short[] {0B00000001, 0B00000010, 0B00000100, 0B00001000, 0B00010000, 0B00100000, 0B01000000, 0B10000000};
	
	static
	{
		for(short i = 0 ; i < BRALLIES.length ; i++)
		{
			BRALLIES[i] = new Pattern(i);
		}
	}
	
	/**
	 * Convert the binary value from ISO 11548-1 to the 8-cell indexing used in this program.
	 * 
	 * @param cell6index:
	 *        The binary representation of the Braille pattern in ISO 11548-1.
	 * @return
	 *        The custom index used in this program.
	 */
	public static short cellIndex6to8(short cell6index)
	{
		return (short)((cell6index & 0B10000111) | ((cell6index & 0B00111000) << 1) | ((cell6index & 0B01000000) >> 3)) ;
	}
	
	/**
	 * Convert the 8-cell indexing used in this program to the binary value in ISO 11548-1.
	 * 
	 * @param cell8index:
	 *        The custom index used in this program.
	 * @return
	 *        The binary representation of the Braille pattern in ISO 11548-1.
	 */
	public static short cellIndex8to6(short cell8index)
	{
		return (short)((cell8index & 0B10000111) | ((cell8index & 0B01110000) >> 1) | ((cell8index & 0B00001000) << 3)) ;
	}
	
	public static Pattern getPattern6(short cell6index)
	{
		return BRALLIES[cellIndex6to8(cell6index)];
	}
	
	/**
	 * Get the Braille character from ISO 11548-1 binary.
	 */
	public static char getBraille6(short cell6index)
	{
		return BRALLIES[cellIndex6to8(cell6index)].braille;
	}
	
	public static Pattern getPattern8(short cell8index)
	{
		return BRALLIES[cell8index];
	}
	
	/**
	 * Get the Braille character from this custom index method.
	 */
	public static char getBraille8(short cell8index)
	{
		return BRALLIES[cell8index].braille;
	}
	
	@Deprecated
	public static Pattern getPattern8(boolean[] dots)
	{
		int cache = 0;
		for(int i = 0 ; i < 8 ; i++)
		{
			if(dots[i])
			{
				cache |= BOOL_INDEX_TO_DOTS[i];
			}
		}
		return BRALLIES[cache];
	}
	
	/*
	 * Internal usage only.
	 */
	@Deprecated
	public static short shiftedShort(int i)
	{
		return BOOL_INDEX_TO_DOTS[i];
	}
	
	/**
	 * Custom holder for handling Braille characters.
	 */
	public static class Pattern
	{
		/** 
		 * (only lower 8 bits used) 8-bit integer with each bit as a boolean value
		 * Using short since Java does not provide unsigned integers.
		 * index = y * 4 + x;
		 * x -> vertical downwards,
		 * y -> horizontal towards the right.
		 */
		private final short cellIndex8;
		
		/**
		 * Braille character.
		 */
		private final char braille;
		
		private Pattern(short index)
		{
			this.cellIndex8 = index;
			this.braille = (char)(0x2800 + BrailleCharacters.cellIndex8to6(index));
		}
		
		public char getBraille()
		{
			return this.braille;
		}
		
		public short getIndex()
		{
			return this.cellIndex8;
		}
	}
}
