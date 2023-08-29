package com.icoderoad.example.demo.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;

import com.google.code.kaptcha.text.WordRenderer;
import com.google.code.kaptcha.util.ConfigHelper;
import com.google.code.kaptcha.util.Configurable;

public class MyRandomColorWordRenderer  extends Configurable implements WordRenderer
{
	ConfigHelper configHelper = new ConfigHelper();
	
	
	public BufferedImage renderWord(String word, int width, int height)
	{
		int fontSize = getConfig().getTextProducerFontSize();
		Font[] fonts = getConfig().getTextProducerFonts(fontSize);
		Color color = getConfig().getTextProducerFontColor();
		int charSpace = getConfig().getTextProducerCharSpace();
		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2D = image.createGraphics();
		g2D.setColor(color);

		RenderingHints hints = new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		hints.add(new RenderingHints(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY));
		g2D.setRenderingHints(hints);

		FontRenderContext frc = g2D.getFontRenderContext();
		Random random = new SecureRandom();

		int startPosY = (height - fontSize) / 5 + fontSize;

		char[] wordChars = word.toCharArray();
		Font[] chosenFonts = new Font[wordChars.length];
		int [] charWidths = new int[wordChars.length];
		int widthNeeded = 0;
		for (int i = 0; i < wordChars.length; i++)
		{
			
			//chosenFonts[i] = new Font("Arial", Font.BOLD, fontSize);
			//random
			chosenFonts[i] = fonts[random.nextInt(fonts.length)];

			char[] charToDraw = new char[]{
				wordChars[i]
			};
			GlyphVector gv = chosenFonts[i].createGlyphVector(frc, charToDraw);
			charWidths[i] = (int)gv.getVisualBounds().getWidth();
			if (i > 0)
			{
				widthNeeded = widthNeeded + 2;
			}
			widthNeeded = widthNeeded + charWidths[i];
		}
		
		HashMap<String,String> selectedColor =new HashMap<String,String>();
		int startPosX = (width - widthNeeded) / 2;
		
		for (int i = 0; i < wordChars.length; i++)
		{
			String randomcolor="";
			do {
				randomcolor=COLOR_LIST[random.nextInt(COLOR_LIST.length)].replaceAll(" ", "");
			}while(selectedColor.containsKey(randomcolor));
			
			selectedColor.put(randomcolor, randomcolor);
			
			color = configHelper.getColor(randomcolor, randomcolor, Color.LIGHT_GRAY);
			g2D.setColor(color);
			
			g2D.setFont(chosenFonts[i]);
			
			char[] charToDraw = new char[] {
				wordChars[i]
			};
			
			//System.out.println(charToDraw[0] +" - "+chosenFonts[i]);
			g2D.drawChars(charToDraw, 0, charToDraw.length, startPosX, startPosY);
			startPosX = startPosX + (int) charWidths[i] + charSpace;
		}
		
		return image;
	}
	
	static String [] COLOR_LIST = {
			
			"0, 0, 0",			//black
			"0, 0, 128",		//navy
			"0, 0, 255",		//blue
			"0, 128, 0",		//green
			"0, 128, 128",		//teal
			"0, 255, 0",		//lime
			"0, 255, 255",		//aqua
			"75, 0, 130",		//Indigo
			"128, 0, 0",		//maroon
			"128, 0, 128",		//purple
			"128, 128, 0",		//olive
			"135, 206, 235",	//SkyBlue`````
			"165, 42, 42",		//Brown
			"210, 105, 30",		//Chocolate
			"255, 0, 0",		//red
			"255, 0, 255",		//fuchsia
			"255, 69, 0",		//OrangeRed
			"255, 127, 80",		//Coral
			"255, 165, 0",		//Orange
			"255, 192, 203",	//Pink
			"255, 215, 0",		//Gold
			"255, 255, 0",		//yellow
	};
}
