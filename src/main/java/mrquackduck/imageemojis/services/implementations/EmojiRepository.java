package mrquackduck.imageemojis.services.implementations;

import mrquackduck.imageemojis.ImageEmojisPlugin;
import mrquackduck.imageemojis.configuration.Configuration;
import mrquackduck.imageemojis.services.abstractions.IEmojiRepository;
import mrquackduck.imageemojis.types.models.EmojiModel;
import mrquackduck.imageemojis.utils.CharUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class EmojiRepository implements IEmojiRepository {
    private final ImageEmojisPlugin plugin;
    private final Configuration config;
    private final Logger logger;
    private List<EmojiModel> cachedEmojis;

    public EmojiRepository(ImageEmojisPlugin plugin) {
        this.plugin = plugin;
        this.config = new Configuration(plugin);
        this.logger = plugin.getLogger();
        this.cachedEmojis = null;
    }

    @Override
    public List<EmojiModel> getEmojis() {
        if (cachedEmojis != null) return cachedEmojis;

        // 이모지 커스텀 경로
        String emojisFolderDirectory = plugin.getDataFolder().getParentFile().getAbsolutePath() + config.emojisFolder();

        File emojisFolder = new File(emojisFolderDirectory);

        File[] files = emojisFolder.listFiles();

        List<EmojiModel> emojis = new ArrayList<>();
        if (files == null) return emojis;

        long rangeStart;
        long rangeEnd;

        if (config.isExtendedUnicodeRangeEnabled()) {
            rangeStart = CharUtil.parseUtf8CodeToLong("\\uE000");
            rangeEnd = rangeStart + 6400;
        }
        else {
            // Setting these values for backward compatibility
            rangeStart = CharUtil.parseUtf8CodeToLong("\\uEff2");
            rangeEnd = rangeStart + 2000;
        }

        int parentCount = 0;
        int childCount = 0;
        int failedCount = 0;
        long currentImageCode = rangeStart;

        File resizedEmojisFolder = new File(plugin.getDataFolder() + "/resized_emojis");

        if (resizedEmojisFolder.exists() && resizedEmojisFolder.isDirectory()) {
            File[] resizedEmojis = resizedEmojisFolder.listFiles();

            if (resizedEmojis != null) {
                for (File resizedEmoji : resizedEmojis) {
                    resizedEmoji.delete();
                }
            }
        }

        for (File file : files) {
            // 한 단계 더 안으로 들어가도록 변경
            logger.info("Loading emojis for " + file.getName());
            if (currentImageCode >= rangeEnd) {
                logger.warning("The maximum number of emojis has been reached.");
                break;
            }
            if (!file.isDirectory()) continue;
            File[] subFiles = file.listFiles();
            if (subFiles == null) return emojis;
            parentCount++;
            for (File subFile : subFiles) {
                if (currentImageCode >= rangeEnd) break;

                if (!subFile.isFile()) continue;
                if (!isPng(subFile)) {
                    failedCount++;
                    continue;
                }

                try {
                    BufferedImage image = ImageIO.read(subFile);
                    if (image == null) continue;

                    File outputFile = subFile;

                    if (image.getWidth() > 256 || image.getHeight() > 256) {
                        // 1. 원본 이미지의 타입 가져오기 (보통 BufferedImage.TYPE_INT_RGB 또는 TYPE_INT_ARGB)
                        int type = image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType();

                        // 2. 새로운 빈 BufferedImage 객체 생성
                        BufferedImage resizedImage = new BufferedImage(256, 256, type);

                        // 3. Graphics2D 객체를 통해 새로운 이미지에 원본 그리기
                        Graphics2D g2d = resizedImage.createGraphics();
                        g2d.drawImage(image, 0, 0, 256, 256, null);
                        g2d.dispose(); // 리소스 해제

                        outputFile = new File(plugin.getDataFolder() + "/resized_emojis/" + (subFile.getName().substring(0, subFile.getName().lastIndexOf('.'))) + "_resized.png");

                        ImageIO.write(resizedImage, "png", outputFile);
                    }

                    childCount++;

                    String iconName = subFile.getParentFile()
                            .getName() + "/" + subFile.getName();
                    String name =  iconName
                            .substring(0, iconName
                                    .lastIndexOf('.'));
                    String fileName = parentCount + "-" + childCount + ".png";

//                    해시 대신 순차 저장하도록 변경
//                    // Generating a hash based on the file name
//                    String fileNameHash = CharUtil.generateSHA256(fileName);
//
//                    // Applying the hash on certain UTF-8 range in order to get a unique UTF-8 code for the emoji
//                    String utf8Code = CharUtil.parseLongToUtf8Code(CharUtil.hashToRange(fileNameHash, rangeStart, rangeEnd));

                    String utf8Code = CharUtil.parseLongToUtf8Code(currentImageCode);
                    currentImageCode++;

                    int height = image.getHeight();
                    String absolutePath = outputFile.getAbsolutePath();

                    EmojiModel emojiModel = new EmojiModel(name, fileName, height, absolutePath, Collections.singletonList(utf8Code), config.templateFormat());
                    emojis.add(emojiModel);
                } catch (IOException e) {
                    logger.warning("Failed to read image file: " + subFile.getName());
                }
            }
            childCount = 0;
            if (failedCount > 0) {
                logger.warning(String.format("Skipping %d files in %s. Only 'png'-native image files are supported. Try converting into 'png'.", failedCount, file.getName()));
                failedCount = 0;
            }
        }


        // Sort emojis by name
        emojis.sort(Comparator.comparing(EmojiModel::getName));

        cachedEmojis = emojis;
        return emojis;
    }

    private boolean isPng(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png");
    }
}
