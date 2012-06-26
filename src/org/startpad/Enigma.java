/*
  Enigma.java - Enigma machine simulation.

  Ported from Enigma.js, Feb, 2010

  References:
   	  Paper Enigma - http://mckoss.com/Crypto/Enigma.htm
   	  Enigma Simulator - http://bit.ly/enigma-machine
   	  Enigma History - http://en.wikipedia.org/wiki/Enigma_machine


 */

package org.startpad;

import java.util.Iterator;
import java.util.ArrayList;

public class Enigma
    {
    static class Rotor
        {
        String name;
        String wires;
        char notch;
        int[] map = new int[26];
        int[] mapReverse = new int[26];

        public Rotor()
            {
            }

        public Rotor(String name, String wires, char notch)
            {
            this.name = name;
            this.wires = wires;
            this.notch = notch;

            this.CreateMapping();
            }

        public String toString()
            {
            return this.name;
            }

        private void CreateMapping()
            {
            for (int iFrom = 0; iFrom < 26; iFrom++)
                {
                int iTo = iFromCh(this.wires.charAt(iFrom));
                this.map[iFrom] = (26 + iTo - iFrom) % 26;
                this.mapReverse[iTo] = (26 + iFrom - iTo) % 26;
                }
            }
        }

    static public class Settings
        {
        public String[] rotors;
        public String reflector;
        public char[] position;
        public char[] rings;
        public String plugs;
        
        public Settings()
            {
            rotors = new String[] { "I", "II", "III" };
            reflector = "B";
            position = new char[] { 'M', 'C', 'K' };
            rings = new char[] { 'A', 'A', 'A' };
            plugs = "";
            }
        }

    public interface Trace
        {
        public void Callback(String trace);
        }

    private Trace trace;

    static Rotor[] rotorsBox = {
            new Rotor("I", "EKMFLGDQVZNTOWYHXUSPAIBRCJ", 'Q'),
            new Rotor("II", "AJDKSIRUXBLHWTMCQGZNPYFVOE", 'E'),
            new Rotor("III", "BDFHJLCPRTXVZNYEIWGAKMUSQO", 'V'),
            new Rotor("IV", "ESOVPZJAYQUIRHXLNFTGKDCMWB", 'J'),
            new Rotor("V", "VZBRGITYUPSDNHLXAWMJQOFECK", 'Z'),
            new Rotor("B", "YRUHQSLDPXNGOKMIEBFZCWVJAT", ' '),
            new Rotor("C", "FVPJIAOYEDRZXWGCTKUQSBNMHL", ' ') };

    // Instance variables

    Settings settings = new Settings();
    Rotor[] rotors = new Rotor[3];
    Rotor reflector;
    public int[] position = new int[3];
    int[] rings = new int[3];
    int[] mapPlugs = new int[26];

    public Enigma(Trace trace)
        {
        this.trace = trace;

        if (this.trace != null)
            this.trace.Callback("Constructed");

        init(settings);
        }

    public void init(Settings settings)
        {
        if (settings != null)
            this.settings = settings;

        for (int i = 0; i < 3; i++)
            this.rotors[i] = rotorFromName(this.settings.rotors[i]);

        this.reflector = rotorFromName(this.settings.reflector);

        for (int i = 0; i < 3; i++)
            this.position[i] = iFromCh(this.settings.position[i]);

        for (int i = 0; i < 3; i++)
            this.rings[i] = iFromCh(this.settings.rings[i]);

        this.settings.plugs = this.settings.plugs.toUpperCase();
        this.settings.plugs = this.settings.plugs.replaceAll("[^A-Z]", "");
        if (this.settings.plugs.length() % 2 == 1)
            throw new IllegalArgumentException(
                    "Plugboard settings must have an even number of letters.");

        for (int i = 0; i < 26; i++)
            this.mapPlugs[i] = i;

        for (int i = 0; i < this.settings.plugs.length(); i += 2)
            {
            int iFrom = iFromCh(this.settings.plugs.charAt(i));
            int iTo = iFromCh(this.settings.plugs.charAt(i + 1));

            if (this.mapPlugs[iFrom] != iFrom)
                throw new IllegalArgumentException(
                        "Redefinition of plug settings for " + chFromI(iFrom));

            if (this.mapPlugs[iTo] != iTo)
                throw new IllegalArgumentException(
                        "Redefinition of plug setting for " + chFromI(iTo));

            this.mapPlugs[iFrom] = iTo;
            this.mapPlugs[iTo] = iFrom;
            }
        }

    public String encode(String s)
        {
        String sOut = "";

        for (int i = 0; i < s.length(); i++)
            sOut += this.encodeChar(s.charAt(i));
        return sOut;
        }

    public char encodeChar(char ch)
        {
        ArrayList<Integer> tracePath = new ArrayList<Integer>();

        ch = Character.toUpperCase(ch);

        if (ch < 'A' || ch > 'Z')
            return ch;

        this.incrementRotors();

        int i = iFromCh(ch);
        tracePath.add(i);

        i = this.mapPlugs[i];
        tracePath.add(i);

        for (int r = 2; r >= 0; r--)
            {
            int d = this.rotors[r].map[(i + this.position[r] - this.rings[r] + 26) % 26];
            i = (i + d) % 26;
            tracePath.add(i);
            }

        i = (i + this.reflector.map[i]) % 26;
        tracePath.add(i);

        for (int r = 0; r < 3; r++)
            {
            int d = this.rotors[r].mapReverse[(i + this.position[r]
                    - this.rings[r] + 26) % 26];
            i = (i + d) % 26;
            tracePath.add(i);
            }

        i = this.mapPlugs[i];
        tracePath.add(i);

        if (this.trace != null)
            {
            String s = "";
            String sSep = "";

            Iterator<Integer> iter = tracePath.iterator();
            while (iter.hasNext())
                {
                s += sSep + chFromI(iter.next());
                sSep = "->";
                }
            trace.Callback(s);
            }

        return chFromI(i);
        }

    private void incrementRotors()
        {
        /*
         * Note that notches are components of the outer rings. So wheel motion
         * is tied to the visible rotor position (letter or number) NOT the
         * wiring position - which is dictated by the rings settings (or offset
         * from the 'A' position).
         */

        // Middle notch - all rotors rotate
        if (this.position[1] == iFromCh(this.rotors[1].notch))
            {
            this.position[0] += 1;
            this.position[1] += 1;
            }
        // Right notch - two rotors rotate
        else if (this.position[2] == iFromCh(this.rotors[2].notch))
            this.position[1] += 1;

        this.position[2] += 1;

        for (int i = 0; i < 3; i++)
            this.position[i] = this.position[i] % 26;
        }

    public String toString()
        {
        String s = "Rotors: ";
        String sSep;

        s += this.reflector.toString();
        sSep = "-";
        for (int i = 0; i < 3; i++)
            s += sSep + this.rotors[i].toString();

        s += " Position: " + sPosition();

        s += " Rings: ";
        for (int i = 0; i < 3; i++)
            s += chFromI(this.settings.rings[i]);

        s += " Plugs: ";
        sSep = "";
        for (int i = 0; i < this.settings.plugs.length(); i += 2)
            {
            s += sSep + this.settings.plugs.substring(i, i + 2);
            sSep = " ";
            }

        return s;
        }
    
    public String sPosition()
	    {
    	String s = "";
        for (int i = 0; i < 3; i++)
            s += chFromI(position[i]);
	    return s;
	    }
    
    public char spinRotor(int iRotor, int dSpin)
	    {
	    position[iRotor] = (position[iRotor] + dSpin + 26) % 26;
	    return chFromI(position[iRotor]);
	    }

    private Rotor rotorFromName(String name)
        {
        for (int i = 0; i < rotorsBox.length; i++)
            if (name.equals(rotorsBox[i].name))
                return rotorsBox[i];
        throw new IllegalArgumentException("Invalid Rotor Name: " + name);
        }

    public static int iFromCh(char ch)
        {
        ch = Character.toUpperCase(ch);
        return ch - 'A';
        }

    public static char chFromI(int i)
        {
        return (char) ((int) 'A' + i);
        }

    public static String groupLetters(String s)
        {
        s = s.toUpperCase();
        s = s.replaceAll("[^A-Z]", "");
        return groupBy(s, 5, ' ');
        }

    private static String groupBy(String s, int n, char c)
        {
        String sOut = "";
        String sSep = "";
        while (s.length() > n)
            {
            sOut += sSep + s.substring(0, n);
            s = s.substring(n);
            sSep = " ";
            }
        if (s.length() > 0)
            sOut += sSep + s;
        return sOut;
        }

    public static void main(String[] args)
        {
        System.out.println("Hello, world");
        for (int i = 0; i < rotorsBox.length; i++)
            System.out.println(rotorsBox[i].name + ": " + rotorsBox[i].wires
                    + "(" + rotorsBox[i].notch + ")");

        Enigma e = new Enigma(new Trace()
            {
                public void Callback(String trace)
                    {
                    System.out.println("Trace: " + trace);
                    }
            });

        // e = new Enigma(null);

        System.out.println("Machine: " + e.toString());

        if (e.trace != null)
            {
            System.out.println("has trace");
            }
        else
            {
            System.out.println("no trace");
            }

        System.out.println(groupBy("abcdefghijklmnop", 5, ' '));

        System.out.println(e.encode("Enigma Revealed!"));
        }

    }
