package org.unicode.tools.emoji;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.tools.emoji.CountEmoji.Attribute;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.tools.emoji.Emoji.Source;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class CopyImagesToCldr {
    private static final boolean VIEW_ONLY = false;
    
    private static final ImmutableSet<Source> DEFAULT_SOURCE_LIST = ImmutableSet.of(
            Source.google, 
            Source.twitter, 
            Source.emojione, 
            Source.sample, 
            Source.proposed);
    
    private static final Map<String,Set<Source>> OVERRIDE_SOURCES = ImmutableMap.<String, Set<Source>>builder()
            .put("🚙", ImmutableSet.<Source>builder().add(Source.emojione).addAll(DEFAULT_SOURCE_LIST).build())
            .build();

    public static void main(String[] args) {
        String targetDir = CLDRPaths.BASE_DIRECTORY + "tools/cldr-apps/WebContent/images/emoji/";
        for (String emoji : EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives()) {
            Category bucket = Category.getBucket(emoji);
            if (bucket.hasAttribute(Attribute.skin) || bucket.hasAttribute(Attribute.hair)) {
                continue;
            }
            File file = getBestPublic(emoji);
            if (file == null) {
                System.out.println("***No image for: " + emoji + ": " + EmojiData.EMOJI_DATA.getName(emoji));
            } else {
                String chars = emoji.replace(Emoji.EMOJI_VARIANT_STRING,"");
                File newName = new File(targetDir, "emoji_" + Emoji.buildFileName(chars, "_") + ".png");
                System.out.println(file.getName() + " => " + newName.getName());
                if (VIEW_ONLY) continue;
                try {
                    Files.copy(file, newName);
                } catch (IOException e) {
                    throw new ICUUncheckedIOException(e);
                }
            }
        }
    }

    private static File getBestPublic(String emoji) {
        Set<Source> sourceList = OVERRIDE_SOURCES.get(emoji);
        if (sourceList == null) {
            sourceList = DEFAULT_SOURCE_LIST;
        } else {
            int debug = 0;
        }
        for (Source source : sourceList) {
            File file = Emoji.getImageFile(source, emoji);
            if (file != null && file.exists()) {
                return file;
            }
        }
        return null;
    }
}
