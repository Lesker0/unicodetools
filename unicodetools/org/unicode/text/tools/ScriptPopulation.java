package org.unicode.text.tools;

import java.util.BitSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.draft.CharacterFrequency;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UScript.ScriptUsage;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class ScriptPopulation {
    static CLDRConfig testInfo = CLDRConfig.getInstance();
    static SupplementalDataInfo supplemental = testInfo.getSupplementalDataInfo();
    static final boolean SHOW_FREQ = false;
    static final Normalizer2 nfkc = Normalizer2.getNFKCInstance();
    static final Normalizer2 NFC = Normalizer2.getNFCInstance();

    // define a category to be:
    // explicit script
    // main general category

    public static void main(String[] args) {
        //    getLanguageInfo();
        //    LanguageTagParser ltp = new LanguageTagParser();
        //    LikelySubtags likely = new LikelySubtags(supplemental);
        //    Counter2<Integer> scriptPopulation = new Counter2<>();
        Counter2<Integer> scriptFrequency = new Counter2<>();
        Counter2<Integer> rawScriptFrequency = new Counter2<>();
        Counter<Integer> freq = CharacterFrequency.getCodePointCounter("mul", true);

        @SuppressWarnings("unchecked")
        Counter2<Integer>[] categoryToTopItems = new Counter2[Category.CODE_LIMIT];
        for (int i = 0; i < categoryToTopItems.length; ++i) {
            categoryToTopItems[i] = new Counter2<>();
        }

        for (int cp : freq){
            long frequency = freq.get(cp);
            rawScriptFrequency.add(UScript.getScript(cp), (double) frequency);

            // quick approximate normalization
            int i = UCharacter.foldCase(cp,true);
            String str = NFC.normalize(UTF16.valueOf(i));
            if (1 == str.codePointCount(0, str.length())) {
                i = str.codePointAt(0);
            }

            int scriptNum = Category.getCategory(i);
            if (scriptNum == Category.Extra.Private.ordinal()) {
                continue;
            }
            scriptFrequency.add(scriptNum, (double)frequency);
            categoryToTopItems[scriptNum]
                    .add(i, (double)frequency);
        }
        // make sure all existing scripts have at least 1
        for (int script = Category.OFFSET; script < Category.CODE_LIMIT; ++script) {
            if (Category.isVariant(script)) {
                continue;
            }
            ScriptUsage usage = Category.getUsage(script);
            if (usage != ScriptUsage.NOT_ENCODED && scriptFrequency.getCount(script) == 0.0d) {
                scriptFrequency.add(script, 1.0d); // fake unknown scripts
                //System.out.println("Adding" + Category.getName(script));
            }
        }
        DecimalFormat nf = (DecimalFormat) NumberFormat.getInstance(ULocale.ENGLISH);
        nf.setMaximumSignificantDigits(3);
        nf.setMinimumSignificantDigits(3);
        double totalFreq = scriptFrequency.getTotal().doubleValue();
        int count = 0;
        System.out.println("№\t-log(%)\tScript (*Cat)\tUAX31 Status\t1st\t2nd\t3rd\t4th\t5th\t6th\t7th\t8th\t9th\t10th\t11th\t12th\t13th\t14th\t15th\t16th\t17th\t18th\t19th\t20th\t…");
        for (Integer category : scriptFrequency.getKeysetSortedByCount(false, null)) {
            Double frequ = scriptFrequency.getCount(category);
            System.out.print(++count
                    + (true ? "\t" + nf.format(Math.log(totalFreq/frequ)) : "")
                    + "\t" + Category.getName(category)  + "\t" + Category.getUsageName(category));
            int max = 20;
            Counter2<Integer> topItems = categoryToTopItems[category];
            for (Integer codePoint : topItems.getKeysetSortedByCount(false, null)) {
                if (--max < 0) {
                    System.out.print("\t…");
                    break;
                }
                Double cFreq = topItems.getCount(codePoint);
                String str = UTF16.valueOf(codePoint);
                if (codePoint == '=' || codePoint == '"' || codePoint == '\'' || codePoint == '+') {
                    str = '\'' + str;
                }
                System.out.print(
                        (SHOW_FREQ ? "\t" + cFreq : "")
                        + "\t" + str);
            };
            System.out.println();
        }
        if (true) return;

        //    Relation<String, String> languagesWithoutScripts = Relation.of(new TreeMap<String,Set<String>>(), TreeSet.class);
        //    for (String language : supplemental.getLanguagesForTerritoriesPopulationData()) {
        //      boolean usedSil = false;
        //      String script = ltp.set(language).getScript();
        //      if (script.isEmpty()) {
        //        String max = likely.maximize(language);
        //        if (max != null) {
        //          script = ltp.set(max).getScript();
        //        } else {
        //          Data dataSet = ExtractDefaultScripts.getBestData(language);
        //          if (dataSet != null) {
        //            script = dataSet.scriptCode;
        //            usedSil = true;
        //          }
        //        }
        //      }
        //      // assume traditional handles bopomofo
        //      boolean bopomofo = false;
        //      if (script.equals("Hant")) {
        //        script = "Han";
        //        bopomofo = true;
        //      } else if (script.equals("Hans")) {
        //        script = "Han";
        //      } else if (script.equals("Kore")) {
        //        script = "Hang";
        //      } else if (script.equals("Jpan")) {
        //        script = "Hiragana";
        //      }
        //      int scriptNum = UScript.getCodeFromName(script);
        //      for (String territory : supplemental.getTerritoriesForPopulationData(language)) {
        //        PopulationData pop = supplemental.getLanguageAndTerritoryPopulationData(language, territory);
        //        double population = pop.getLiteratePopulation();
        //        scriptPopulation.add(scriptNum, population);
        //        if (scriptNum == UScript.HIRAGANA) {
        //          scriptPopulation.add(UScript.KATAKANA, population);
        //          scriptPopulation.add(UScript.HAN, population);
        //        } else if (bopomofo) {
        //          scriptPopulation.add(UScript.BOPOMOFO, population);
        //        } else if (scriptNum == UScript.UNKNOWN || usedSil) {
        //          languagesWithoutScripts.put(testInfo.getEnglish().getName(language), 
        //              language 
        //              + "\t" + script 
        //              + "\t" + territory 
        //              + "\t" + (long)population
        //              + "\t" + Category.getUsage(scriptNum)
        //              );
        //        }
        //      }
        //    }
        //    for (Entry<String, String> lang : languagesWithoutScripts.entrySet()) {
        //      System.out.println(lang.getKey() + "\t" + lang.getValue());
        //    }
        //    for (int i = 0; i < Category.CODE_LIMIT; ++i) {
        //      ScriptUsage usage = Category.getUsage(i);
        //      if (usage == ScriptUsage.NOT_ENCODED) {
        //        continue;
        //      }
        //      long charCount = scriptFrequency.get(i);
        //      long count = (long) scriptPopulation.getCount(i).doubleValue();
        //      if (charCount == 0 && count == 0) {
        //        continue;
        //      }
        //      if (count < 1) {
        //        count = 1;
        //      }
        //      if (charCount < 1) {
        //        charCount = 1;
        //      }
        //      System.out.println(Category.getName(i) + "\t" + usage + "\t" + count + "\t" + charCount);
        //    }
        //    BitSet bitset = new BitSet();
        //    UnicodeMap<String> fixedScripts = new UnicodeMap<>();
        //    UnicodeMap<String> diffScripts = new UnicodeMap<>();
        //
        //    for (int i = 0; i < 0x110000; ++i) {
        //      if (i == 0x0363) {
        //        int debug = 0;
        //      }
        //      int scx = UScript.getScriptExtensions(i, bitset);
        //      int sc = Category.getCategory(i);
        //      if (scx >= 0) { // single script
        //        continue;
        //      }
        //      String scriptNames = getScriptNames(bitset, " ", false);
        //      String scriptName = Category.getName(sc);
        //      //System.out.println(scriptName + "\t" + scriptNames + "\t" + Utility.hex(i) + "\t" + UCharacter.getName(i));
        //      if (sc == UScript.COMMON || sc == UScript.INHERITED) {
        //        int single = getBest(bitset);
        //        if (single > 0) {
        //          fixedScripts.put(i, 
        //              scriptName 
        //              + ";" + Category.getName(single) 
        //              + ";" + scriptNames);
        //        }
        //        continue;
        //      }
        //      diffScripts.put(i, scriptName + ";«same»;" + scriptNames);
        //    }
        //    System.out.println("\nDiff Scripts\n");
        //    showScripts(diffScripts);
        //    System.out.println("\nFixed Scripts\n");
        //    showScripts(fixedScripts);
    }

    static final UnicodeSet SHOULD_BE_SYMBOL = new UnicodeSet("[@ * \\& # % ‰ ‱ † ‡ ※]").freeze(); // PRI 228
    static final UnicodeSet SHOULD_BE_GREEK = new UnicodeSet("[ℼ µℽ ʹ  ̓  ̈́]").freeze();
    static final UnicodeSet SHOULD_BE_COPTIC = new UnicodeSet("[\uFE24-\uFE26]").freeze();
    static final UnicodeSet SHOULD_BE_DEVA = new UnicodeSet("[᳓ ᳩ-ᳬᳮ-ᳱ ᳵ ᳶ]").freeze();
    static final UnicodeSet latinMark = new UnicodeSet("[[:scx=common:][:scx=inherited:]&[:mark:]"
            + "-[:variationselector:]"
            + "-[\u034F]" // grapheme joiner
            + "-[\\x{101FD}]"  // Phaistos Disc
            + "-[:block=Musical Symbols:]"
            + "]")
    .removeAll(SHOULD_BE_GREEK)
    .removeAll(SHOULD_BE_COPTIC)
    .removeAll(SHOULD_BE_DEVA).freeze();
    static final UnicodeSet latinLetter = new UnicodeSet("[[:scx=common:][:scx=inherited:]&[:letter:]]")
    .removeAll(SHOULD_BE_GREEK)
    .removeAll(SHOULD_BE_COPTIC)
    .removeAll(SHOULD_BE_DEVA)
    .complement().complement();
    static final UnicodeSet SHOULD_BE_LATIN = new UnicodeSet(latinMark).addAll(latinLetter).freeze();
    static final UnicodeSet SHOULD_BE_HAN = new UnicodeSet("[[:East_Asian_Width=Fullwidth:]\\p{Block=Counting Rod Numerals}-[:cn:]]").freeze();
    static final UnicodeSet SHOULD_BE_KANA = new UnicodeSet("[・]").freeze();
    static final UnicodeSet SHOULD_BE_PUNCTUATION = new UnicodeSet("[`´]").freeze();

    static class Category {
        enum Extra {Unknown, Whitespace, Letter, Mark, Numeric, Control, Format, Punctuation, Symbol, Private};

        private static final Extra[] ITEMS = Extra.values();
        private static final int OFFSET = ITEMS.length;

        public static final int CODE_LIMIT = UScript.CODE_LIMIT;

        private static String getUsageName(Integer category) {
            ScriptUsage usage = Category.getUsage(category);
            return usage == ScriptUsage.UNKNOWN ? "N/A" : UCharacter.toTitleCase(usage.toString(), null);
        }

        public static String getName(int category) {
            return category < OFFSET 
                    ? "*General " + ITEMS[category].toString()
                            : category - OFFSET == UScript.HIRAGANA
                            ? "Kana"
                                    : UScript.getName(category - OFFSET);
        }

        public static ScriptUsage getUsage(int category) {
            return category < OFFSET 
                    ? UScript.ScriptUsage.UNKNOWN
                            : UScript.getUsage(category - OFFSET);
        }

        private static boolean isVariant(int category) {
            int script = category - Category.OFFSET;
            return 
                    script == UScript.SIMPLIFIED_HAN 
                    || script == UScript.TRADITIONAL_HAN
                    || script == UScript.UNKNOWN
                    || script == UScript.JAPANESE
                    || script == UScript.KOREAN
                    || script == UScript.COMMON
                    || script == UScript.INHERITED
                    || script == UScript.KATAKANA
                    ;
        }

        static final BitSet temp = new BitSet();
        static final StringBuilder buffer = new StringBuilder();

        public static int getCategory(int cp) {

            // first do script

            // more efficient would be a UnicodeMap, but we don't care...
            if (SHOULD_BE_GREEK.contains(cp)) {
                return fixScript(UScript.GREEK);
            } else if (SHOULD_BE_COPTIC.contains(cp)) {
                return fixScript(UScript.COPTIC);
            } else if (SHOULD_BE_DEVA.contains(cp)) {
                return fixScript(UScript.DEVANAGARI);
            } else if (SHOULD_BE_LATIN.contains(cp)) {
                return fixScript(UScript.LATIN);
            } else if (SHOULD_BE_HAN.contains(cp)) {
                return fixScript(UScript.HAN);
            } else if (SHOULD_BE_KANA.contains(cp)) {
                return fixScript(UScript.HIRAGANA);
            }


            temp.clear();
            int script = UScript.getScript(cp);
            if (script != UScript.UNKNOWN && script != UScript.COMMON && script != UScript.INHERITED) {
                return fixScript(script);
            }
            script = getBestScript(cp);
            if (script != UScript.UNKNOWN && script != UScript.COMMON && script != UScript.INHERITED) {
                return fixScript(script);
            }
            buffer.setLength(0);
            buffer.appendCodePoint(cp);
            String nfkcForm = nfkc.normalize(buffer);
            if (nfkcForm.codePointCount(0, nfkcForm.length()) == 1) {
                script = getBestScript(nfkcForm.codePointAt(0));
                if (script != UScript.UNKNOWN && script != UScript.COMMON && script != UScript.INHERITED) {
                    return fixScript(script);
                }
            }


            // now do category

            if (UCharacter.isWhitespace(cp)) {
                return Extra.Whitespace.ordinal();
            }
            if (SHOULD_BE_SYMBOL.contains(cp)) {
                return Extra.Symbol.ordinal();
            } else if (SHOULD_BE_PUNCTUATION.contains(cp)) {
                return Extra.Punctuation.ordinal();
            }

            int category = UCharacter.getType(cp);
            switch (category) {
            case UCharacter.UPPERCASE_LETTER :
            case UCharacter.LOWERCASE_LETTER :
            case UCharacter.TITLECASE_LETTER :
            case UCharacter.MODIFIER_LETTER :
            case UCharacter.OTHER_LETTER :
                return Extra.Letter.ordinal();

            case UCharacter.NON_SPACING_MARK :
            case UCharacter.ENCLOSING_MARK : 
            case UCharacter.COMBINING_SPACING_MARK :
                return Extra.Mark.ordinal();

            case UCharacter.DECIMAL_DIGIT_NUMBER :
            case UCharacter.LETTER_NUMBER :
            case UCharacter.OTHER_NUMBER :
                return Extra.Numeric.ordinal();

            case UCharacter.SPACE_SEPARATOR:
            case UCharacter.LINE_SEPARATOR :
            case UCharacter.PARAGRAPH_SEPARATOR :
                return Extra.Whitespace.ordinal();

            case UCharacter.CONTROL :
                return Extra.Control.ordinal();

            case UCharacter.FORMAT :
                return Extra.Format.ordinal();

            case UCharacter.UNASSIGNED:
            case UCharacter.SURROGATE :
                return Extra.Unknown.ordinal();

            case UCharacter.PRIVATE_USE :
                return Extra.Private.ordinal();

            case UCharacter.DASH_PUNCTUATION :
            case UCharacter.START_PUNCTUATION :
            case UCharacter.END_PUNCTUATION :
            case UCharacter.CONNECTOR_PUNCTUATION :
            case UCharacter.OTHER_PUNCTUATION :
            case UCharacter.INITIAL_PUNCTUATION :
            case UCharacter.FINAL_PUNCTUATION :
                return Extra.Punctuation.ordinal();

            case UCharacter.MATH_SYMBOL :
            case UCharacter.CURRENCY_SYMBOL :
            case UCharacter.MODIFIER_SYMBOL :
            case UCharacter.OTHER_SYMBOL :
                return Extra.Symbol.ordinal();

            default:
                throw new IllegalArgumentException();
            }
        }

        private static int getBestScript(int cp) {
            temp.clear();
            UScript.getScriptExtensions(cp, temp);
            if (temp.get(UScript.HAN)) {
                return UScript.HAN;
            }
            return getBest(temp);
        }

        private static int fixScript(int script) {
            if (script == UScript.KATAKANA) {
                script = UScript.HIRAGANA;
            }
            script += OFFSET;
            return script;
        }

        private static int getBest(BitSet bitset) {
            int best = -1;
            ScriptUsage bestUsage = null;
            for (int i = bitset.nextSetBit(0); i >= 0; i = bitset.nextSetBit(i+1)) {
                ScriptUsage usage = UScript.getUsage(i);
                if (bestUsage == null || usage.compareTo(bestUsage) > 0) {
                    best = i;
                    bestUsage = usage;
                }
            }   
            return best;
        }
    }

    //  private static void showScripts(UnicodeMap<String> fixedScripts) {
    //    String lastS = "";
    //    for (String s : new TreeSet<String>(fixedScripts.getAvailableValues())) {
    //      String[] parts = s.split(";");
    //      if (!lastS.equals(s)) {
    //        System.out.println("\n# " +
    //            "old-sc=" + parts[0] 
    //                + ", " +
    //                "new-sc=" + parts[1] 
    //                    + ", " +
    //                    "scx={" + parts[2] + "}");
    //        lastS = s;
    //      }
    //      for (UnicodeSetIterator it = new UnicodeSetIterator(fixedScripts.getSet(s)); it.nextRange();) {
    //        if (it.codepoint != it.codepointEnd) {
    //          System.out.println(Utility.hex(it.codepoint) + ".." + Utility.hex(it.codepointEnd) 
    //              + " ;\t" + parts[0] + "\t# " 
    //              + UCharacter.getName(it.codepoint) + ".." + UCharacter.getName(it.codepointEnd));
    //        } else {
    //          System.out.println(Utility.hex(it.codepoint) 
    //              + " ;\t" + parts[0] + "\t# " 
    //              + UCharacter.getName(it.codepoint));
    //        }
    //      }
    //    }
    //  }

    //  // should be method on UScript
    //  private static String getScriptNames(BitSet bitset, String separator, boolean shortName) {
    //    StringBuilder result = new StringBuilder();
    //    for (int i = bitset.nextSetBit(0); i >= 0; i = bitset.nextSetBit(i+1)) {
    //      if (result.length() > 0) {
    //        result.append(separator);
    //      }
    //      result.append(Category.getName(i));
    //    }
    //    return result.toString();
    //  }

    //  private static void getLanguageInfo() {
    //    supplemental.getLanguages();
    //    for (String s : 
    //      "sq hy az my ka kk km lo mk mn ne si ky pa uz".split(" ")) {
    //      PopulationData data = supplemental.getBaseLanguagePopulationData(s);
    //      if (data == null) {
    //        System.out.println(
    //            s
    //            + "\t" + testInfo.getEnglish().getName(s)
    //            + "\t" + "NO DATA"
    //            );
    //      } else {
    //        System.out.println(
    //            s
    //            + "\t" + testInfo.getEnglish().getName(s)
    //            + "\t" + data.getPopulation()
    //            + "\t" + data.getLiteratePopulation()
    //            + "\t" + data.getGdp()
    //            );
    //      }
    //    }
    //  }
}
