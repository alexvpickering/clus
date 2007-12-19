/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.io;

import jeans.util.*;

import clus.main.*;

import java.io.*;
import java.util.zip.ZipInputStream;

public class ClusReader {

	String m_Name;
	int m_Row = 0, m_Attr = 0, m_LastChar = 0;
	MStreamTokenizer m_Token;
	StringBuffer m_Scratch = new StringBuffer();
	Settings m_Settings;
	boolean m_IsClosed;
	
	public ClusReader(String fname, Settings sett) throws IOException {
		m_Name = fname;
		m_Settings = sett;
		m_IsClosed = true;
		open();
	}
	
	public String getName() {
		return m_Name;
	}
	
	public int getRow() {
		return m_Row;
	}
	
	public int countRows() throws IOException {
		int nbr = countRows2();
		reOpen();
		return nbr;
	}

	public MStreamTokenizer zipOpen(String fname) throws IOException {
		ZipInputStream zip = new ZipInputStream(new FileInputStream(fname));
		zip.getNextEntry();
		return new MStreamTokenizer(zip);
	}
	
	public void open() throws IOException {
		String fname = m_Name;
		if (m_Settings != null) m_Settings.getFileAbsolute(m_Name);
		if (FileUtil.fileExists(fname)) {
			if (fname.toUpperCase().endsWith(".ZIP")) {
				m_Token = zipOpen(fname);
			} else {
				m_Token = new MStreamTokenizer(fname);
			}
		} else {
			String zipname = fname+".zip";
			if (FileUtil.fileExists(zipname)) {
				m_Token = zipOpen(zipname);				
			} else {
				throw new FileNotFoundException("'"+fname+"'");
			}
		}
		m_Token.setCommentChar('%');
		m_IsClosed = false;
	}

	public boolean isClosed() {
		return m_IsClosed;
	}
	
	public void tryReOpen() throws IOException {
		if (isClosed()) reOpen();
	}	
	
	public void reOpen() throws IOException {
		close();	
		open();
	}

	public void close() throws IOException {
		m_Token.close();
		m_IsClosed = true;
	}	
	
	public MStreamTokenizer getTokens() {
		return m_Token;
	}
	
	public boolean isNextToken(String token) throws IOException {
		return m_Token.isNextToken(token);
	}
	
	public boolean hasMoreTokens() throws IOException {
		Reader reader = m_Token.getReader();
		int ch = reader.read();
		setLastChar(ch);
		return ch != -1;		
	}
	
	public boolean isEol() throws IOException {
		Reader reader = m_Token.getReader();	
		int ch = getNextChar(reader);
		if (ch == 10 || ch == 13) return true;
		setLastChar(ch);
		return false;
	}
	
	public void setLastChar(int ch) {
		m_LastChar = ch;
	}
	
	public int getNextChar(Reader reader) throws IOException {
		if (m_LastChar != 0) {
			int ch = m_LastChar;
			m_LastChar = 0;
			return ch;
		}
		return reader.read();		
	}
	
	public int getNextChar() throws IOException {
		return getNextChar(m_Token.getReader());
	}
	
	public boolean isNextChar(int ch) throws IOException {
		int found = getNextChar();
		if (ch == found) return true;
		setLastChar(found);
		return false;
	}
	
	public void ensureNextChar(int ch) throws IOException {
		int found = getNextChar();
		if (ch != found) {
			throw new IOException("Character '"+(char)ch+"' expected on row "+m_Row+", not '"+(char)found+"'");
		}
	}
		
	public void readEol() throws IOException {
		boolean allowall = false;
		Reader reader = m_Token.getReader();
		int ch = getNextChar(reader);
		while (ch != -1) {
			if (ch == 10 || ch == 13) {
				m_Attr = 0;
				m_Row++;
				break;
			} else if (ch == '%') {
				allowall = true;
			} else if (ch != ' ' && ch != '\t' && allowall == false) {
				throw new IOException("Too many data on row "+m_Row+": '"+(char)ch+"'");
			}
			ch = reader.read();
		}		
	}
	
	public void readTillEol() throws IOException {
		Reader reader = m_Token.getReader();
		int ch = getNextChar(reader);
		while (ch != -1) {
			if (ch == 10 || ch == 13) {
				setLastChar(13);
				break;
			}
			ch = reader.read();
		}
	}
	
	// TODO: add better support for quotes?
	public String readString() throws IOException {
		int nb = 0;
		Reader reader = m_Token.getReader();
		m_Scratch.setLength(0);
		int ch = getNextChar(reader);
		while (ch != -1 && ch != ',' && ch != '}') {
			if (ch == '%') {
				readTillEol();
				break;
			}
			if (ch != '\t' && ch != 10 && ch != 13) {
				m_Scratch.append((char)ch);
				if (ch != ' ') nb++; 
			} else {
				if (ch == 10 || ch == 13) setLastChar(13);
				if (nb > 0) break;
			}
			ch = reader.read();
		}
		if (ch == '}') setLastChar(ch);
		String result = m_Scratch.toString().trim();
		if (result.length() > 0) {
			m_Attr++;			
			return result;
		} else {
			throw new IOException("Error reading attirbute "+m_Attr+" at row "+(m_Row+1));
		}
	}
	
	public void readScratchNoSpace() throws IOException {
		int nb = 0;		
		Reader reader = m_Token.getReader();
		m_Scratch.setLength(0);
		int ch = getNextChar(reader);
		while (ch != -1 && ch != ',' && ch != '}') {
			if (ch != ' ' && ch != '\t' && ch != 10 && ch != 13) {
				if (ch != '\'' && ch != '"') {
					m_Scratch.append((char)ch);
					nb++;
				}
			} else {
				if (ch == 10 || ch == 13) setLastChar(13);
				if (nb > 0) break;
			}
			ch = reader.read();
		}
		if (ch == '}') setLastChar(ch);
	}
	
	public double readFloat() throws IOException {
		readScratchNoSpace();
		if (m_Scratch.length() > 0) {
			m_Attr++;
			String value = m_Scratch.toString();			
			try {	
				if (value.equals("?")) return Double.POSITIVE_INFINITY;			
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				throw new IOException("Error parsing numeric value '"+value+"' for attribute "+m_Attr+" at row "+(m_Row+1));
			}		
		} else {
			throw new IOException("Error reading numeric attirbute "+m_Attr+" at row "+(m_Row+1));
		}
	}

	public int readIntIndex() throws IOException {		
		readScratchNoSpace();
		if (m_Scratch.length() > 0) {
			String value = m_Scratch.toString();			
			try {	
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				throw new IOException("Error parsing integer index '"+value+"' at row "+(m_Row+1));
			}		
		} else {
			throw new IOException("Error: empty index at row "+(m_Row+1));
		}
	}
		
	//TimeSeries attribute can be placed anywhere
	public String readTimeSeries() throws IOException {
		Reader reader = m_Token.getReader();
		m_Scratch.setLength(0);
		int ch = getNextChar(reader);
		int prev = ch;
		while ((ch != -1) && (prev !=']')) {		
			if (ch !=(int) '\t' && ch != 10 && ch != 13) {
				m_Scratch.append((char)ch);
 			} else {
				if (ch == 10 || ch == 13) setLastChar(13);
			}
			prev = ch;
			ch = reader.read();
		}
		
//		if (ch==']')
//			m_Scratch.append((char)ch);	

		if (ch == -1)
			m_Scratch.append((char)prev);	

		String result = m_Scratch.toString().trim();
		if (result.length() > 0) {
			m_Attr++;			
			return result;
		} else {
			throw new IOException("Error reading attirbute "+m_Attr+" at row "+(m_Row+1));
		}
	}	
	
	//--This is the new method which skips the whole time serie(when TimeSeries attribute is disabled) and the reference character is '['

	public void skipTillComma() throws IOException {		
			int nb = 0;
			boolean is_ts=false;
			Reader reader = m_Token.getReader();
			int ch = getNextChar(reader);
			while (ch != -1 && ch != ',') {
				if (ch != ' ' && ch != '\t' && ch != 10 && ch != 13) {
					if (ch=='['){
						is_ts=true;
						break;
					}
					nb++; 
				} else {
					if (ch == 10 || ch == 13) setLastChar(13);
					if (nb > 0) break;
				}
				ch = reader.read();
			}
			int prev=ch;
			while (ch != -1 && prev != ']' && is_ts) {
				if (ch != ' ' && ch != '\t' && ch != 10 && ch != 13) {
					nb++; 
				} else {
					if (ch == 10 || ch == 13) setLastChar(13);
					if (nb > 0) break;
				}
				prev=ch;
				ch = reader.read();
			}
	}
	
	public int countRows2() throws IOException {
		int nbrows = 0;
		int nbchars = 0;
		Reader reader = m_Token.getReader();
		int ch = reader.read();
		while (ch != -1) {
			if (ch == 10 || ch == 13) {
				if (nbchars > 0) nbrows++;
				nbchars = 0;
			} else if (ch != ' ' && ch != '\t') {
				nbchars++;
			}
			ch = reader.read();
		}
		// Sometimes it is useful to have empty files :-)
		// if (nbrows == 0) throw new IOException("Empty @data section in ARFF file");
		return nbrows;
	}

	public boolean ensureAtEnd() throws IOException {
		Reader reader = m_Token.getReader();
		int ch = reader.read();
		while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
			ch = reader.read();
		}
		return ch == -1;
	}
}
