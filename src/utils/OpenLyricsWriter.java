/*
 * Author: Bo Maryniuk <bo@suse.de>
 *
 * Copyright (c) 2013 Bo Maryniuk. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *     3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY BO MARYNIUK "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package utils;

import de.suse.lib.openlyrics.Chord;
import de.suse.lib.openlyrics.OpenLyricsObject;
import de.suse.lib.openlyrics.Verse;
import de.suse.lib.openlyrics.VerseLine;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Store OpenLyrics object into XML DOM.
 *
 * @author bo
 */
public class OpenLyricsWriter {
    private OpenLyricsObject ol;
    private Document doc;

    /**
     * IO Exception of the OpenLyrics Writer object when file exists.
     */
    public static class OpenLyricsWriterFileExistsException extends IOException {
        public OpenLyricsWriterFileExistsException(String message) {
            super(message);
        }
    }


    /**
     * IO Exception of the OpenLyrics Write object when file cannot be written.
     */
    public static class OpenLyricsWriterWriteErrorException extends IOException {
        public OpenLyricsWriterWriteErrorException(String message) {
            super(message);
        }
    }


    /**
     * Constructor.
     * 
     * @param ol
     * @throws ParserConfigurationException
     */
    public OpenLyricsWriter(OpenLyricsObject ol) 
            throws ParserConfigurationException {
        this.doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        this.ol = ol;
        this.createDOM();
    }


    /**
     * Write the XML into the file on the filesystem.
     *
     * @param xfile
     */
    public void writeToFile(File xfile, boolean overwrite)
            throws OpenLyricsWriterFileExistsException,
                   OpenLyricsWriterWriteErrorException,
                   TransformerConfigurationException,
                   TransformerException {
        if (!overwrite && xfile.exists()) {
            throw new OpenLyricsWriterFileExistsException(String.format("The file %s exists.", xfile.getAbsolutePath()));
        }

        if (overwrite && xfile.exists() && !xfile.canWrite()) {
            throw new OpenLyricsWriterWriteErrorException(String.format("Write access denied file %s exists.", xfile.getAbsolutePath()));
        }

        // Write XML
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMSource source = new DOMSource(this.doc);
        StreamResult result = new StreamResult(xfile);

        //transformer.transform(source, result);
        transformer.transform(source, new StreamResult(System.out));
    }


    /**
     * Create DOM from the OpenLyrics object.
     */
    private void createDOM() {
        // Root
        Element songElement = this.doc.createElement("song");
        songElement.setAttribute("xmlns", "http://openlyrics.info/namespace/2009/song");
        songElement.setAttribute("createdIn", "JOpenLyricsLib");
        songElement.setAttribute("modifiedDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(new Date()));
        this.doc.appendChild(songElement);

        // Assemble properties
        songElement.appendChild(this.getProperties());

        // Assemble lyrics
        songElement.appendChild(this.getLyrics());
    }


    /**
     * Get all the properties.
     *
     * @param properties
     */
    private Element getProperties() {
        Element properties = this.doc.createElement("properties");
        return properties;
    }


    /**
     * Get all the lyrics with chords.
     * 
     * @return
     */
    private Element getLyrics() {
        Element lyrics = this.doc.createElement("lyrics");

        // Get verses
        for (int i = 0; i < this.ol.getVerses().size(); i++) {
            Verse verse = this.ol.getVerses().get(i);

            // Create verse element
            Element verseElement = this.doc.createElement("verse");
            if (verse.getName() != null) {
                verseElement.setAttribute("name", verse.getName());
            }

            // Add lines to the verse
            for (int j = 0; j < verse.getLines().size(); j++) {
                VerseLine line = verse.getLines().get(j);

                // Create line element with chords
                int substroffset = 0;
                for (int cidx = 0; cidx < line.getChords().size(); cidx++) {
                    Chord chord = line.getChords().get(cidx);
                    verseElement.appendChild(this.doc.createTextNode(line.getText().substring(substroffset, chord.getLineOffset())));
                    substroffset = chord.getLineOffset();

                    // Add chord to DOM
                    Element chordElement = this.doc.createElement("chord");
                    chordElement.setAttribute("name", chord.getRoot()); // XXX: Needs a proper chord render
                    verseElement.appendChild(chordElement);
                }
                // Add the rest of the text left after chords.
                verseElement.appendChild(this.doc.createTextNode(line.getText().substring(substroffset)));

                // Do not <br/> to the last line in the verse
                if ((j + 1) < verse.getLines().size()) {
                    verseElement.appendChild(this.doc.createElement("br"));
                }
            }

            // Add verse to DOM
            lyrics.appendChild(verseElement);
        }

        return lyrics;
    }
}







