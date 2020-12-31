package tangent65536.braille;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * Copyright (c) 2020, Tangent65536.
 *  All rights reserved.
 * 
 * The transformer class for converting images to Braille representation.
 */
public class ImageTransformer
{
	/**
	 * Letter-spacing in unit of dots.
	 */
	protected final int tracking;
	
	/**
	 * Line-spacing in unit of dots.
	 */
	protected final int leading;
	
	/**
	 * Width in unit of characters.
	 */
	protected final int width;
	
	/**
	 * Height in unit of characters.
	 */
	protected final int height;
	
	/**
	 * Darkness threshold for plotting a dot.
	 */
	protected final double threshold;
	
	/**
	 * Weight for pixels sitting between dots.
	 */
	protected final double edgeWeight;
	
	/**
	 * Create a transformer handle.
	 * 
	 * @param _w Width of the output in unit of characters.
	 * @param _h Height of the output in unit of characters.
	 * @param _track Desired letter-spacing in unit of dots.
	 * @param _lead Desired line-spacing in unit of dots.
	 * @param _thres Darkness threshold for plotting a dot. The darkness is calculated based on the linear color space sitting between 0 (white) and 1 (dark). 
	 * @param _edge Weight for pixels sitting between dots. Each dot is treated as a 2 x 2 chunk with an 1-pixel wide rim.
	 */
	public ImageTransformer(int _w, int _h, int _track, int _lead, double _thres, double _edge)
	{
		this.tracking = _track;
		this.leading = _lead;
		
		this.width = _w;
		this.height = _h;
		
		this.threshold = _thres;
		this.edgeWeight = _edge;
	}
	
	/**
	 * Transform an image.
	 * 
	 * @param rawImage The input image, which will be rescaled to hard-coded size based on the specified character width and height. The mentioned rims of dots at the edge of the image is omitted.
	 * @return Braille representation with CR-LF newline.
	 */
	public char[] transform(final BufferedImage rawImage)
	{
		char[] ret = new char[(this.width + 2) * this.height]; // width + 2 to add \r\n
		final BufferedImage scaledImage = resize(rawImage, (this.width * 2 + this.tracking * (this.width - 1)) * 3 - 1, (this.height * 4 + this.leading * (this.height - 1)) * 3 - 1);
		
		int xStep = 6 + this.tracking * 3;
		int yStep = 12 + this.leading * 3;
		int retIndex = 0;
		int braille;
		
		for(int y = 0 ; y < scaledImage.getHeight() ; y += yStep)
		{
			for(int x = 0 ; x < scaledImage.getWidth() ; x += xStep)
			{
				braille = 0;
				for(int i = 0 ; i < 4 ; i++)
				{
					for(int j = 0 ; j < 2 ; j++)
					{
						braille |= checkChunkBrightness(scaledImage, x + j * 3, y + i * 3, i + j * 4);
					}
				}
				
				ret[retIndex] = BrailleCharacters.getBraille8((short)braille);
				retIndex++;
			}
			
			ret[retIndex] = '\r';
			ret[retIndex + 1] = '\n';
			
			retIndex += 2;
		}
		
		return ret;
	}
	
	/**
	 * Transform an image to HTML.
	 * 
	 * @param rawImage The input image, which will be rescaled to hard-coded size based on the specified character width and height. Please note that both tracking and leading will be 0 in this mode. The mentioned rims of dots at the edge of the image is omitted.
	 * @return Braille representation in an HTML-encoded String.
	 */
	public String transformCompactHtml(final BufferedImage rawImage)
	{
		final StringBuilder builder = new StringBuilder();
		
		builder.append("<head>");
		builder.append("<style>");
		builder.append("body { font: normal 12px/1.1em monospace; display: block; margin: 1em; white-space: nowrap; } ");
		builder.append("body > span { display: inline-block; width: 0.5em; }");
		builder.append("</style>");
		builder.append("</head>");
		builder.append("<body>");
		
		final BufferedImage scaledImage = resize(rawImage, this.width * 6 - 1, this.height * 12 - 1);
		
		int xStep = 6;
		int yStep = 12;
		int braille;
		
		for(int y = 0 ; y < scaledImage.getHeight() ; y += yStep)
		{
			for(int x = 0 ; x < scaledImage.getWidth() ; x += xStep)
			{
				braille = 0;
				for(int i = 0 ; i < 4 ; i++)
				{
					for(int j = 0 ; j < 2 ; j++)
					{
						braille |= checkChunkBrightness(scaledImage, x + j * 3, y + i * 3, i + j * 4);
					}
				}
				
				builder.append("<span>");
				builder.append(BrailleCharacters.getBraille8((short)braille));
				builder.append("</span>");
			}
			
			builder.append("<br>");
		}
		
		builder.append("</body>");
		
		return builder.toString();
	}
	
	/**
	 * Check the darkness of each chunk and return the result.
	 */
	@SuppressWarnings("deprecation")
	protected short checkChunkBrightness(final BufferedImage img, final int xStart, final int yStart, final int index)
	{
		int x, y, color;
		boolean inDot;
		double divi = 0D, darkness, cache = 0D;
		for(int dx = -1 ; dx < 3 ; dx++)
		{
			x = xStart + dx;
			if(x < 0 || x >= img.getWidth())
			{
				continue;
			}
			
			for(int dy = -1 ; dy < 3 ; dy++)
			{
				y = yStart + dy;
				if(y < 0 || y >= img.getHeight())
				{
					continue;
				}
				
				inDot = ((dx & 0xFFFFFFFE) == 0 && (dy & 0xFFFFFFFE) == 0);
				color = img.getRGB(x, y);
				darkness = 1D - (((color & 0xFF0000) >> 16) * 0.2126 + ((color & 0x00FF00) >> 8) * 0.7152 + (color & 0x0000FF) * 0.0722) / 255D;
				if(inDot)
				{
					cache += darkness;
					divi += 1D;
				}
				else
				{
					cache += darkness * this.edgeWeight;
					divi += this.edgeWeight;
				}
			}
		}
		
		cache /= divi;
		
		if(cache >= this.threshold)
		{
			return BrailleCharacters.shiftedShort(index);
		}
		
		return 0;
	}
	
	/**
	 * Resize an image.
	 */
	protected static BufferedImage resize(BufferedImage img, int newW, int newH)
	{ 
	    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
	    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g2d = dimg.createGraphics();
	    g2d.drawImage(tmp, 0, 0, null);
	    g2d.dispose();

	    return dimg;
	}
}
