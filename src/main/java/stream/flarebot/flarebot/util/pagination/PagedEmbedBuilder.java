package stream.flarebot.flarebot.util.pagination;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

public class PagedEmbedBuilder<T> {
    private String title;
    private String codeBlock;
    private PaginationList<T> list;

    /**
     * Instantiates the builder with a {@link PaginationList}.
     *
     * @param list The {@link PaginationList} to use as the pages.
     */
    public PagedEmbedBuilder(PaginationList<T> list) {
        this.list = list;
    }

    /**
     * Sets the title for the Embed.
     *
     * @param title The title.
     * @return this.
     */
    public PagedEmbedBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the type of code block to use.
     *
     * @param codeBlock The string representing the code block.
     * @return this
     */
    public PagedEmbedBuilder setCodeBlock(String codeBlock) {
        this.codeBlock = codeBlock;
        return this;
    }

    /**
     * Enables code blocks
     * If you use {@link PagedEmbedBuilder#setCodeBlock(String)} it auto enables.
     *
     * @return this
     */
    public PagedEmbedBuilder setCodeBlock() {
        this.codeBlock = "";
        return this;
    }

    /**
     * Builds a {@link PagedEmbed} for use in embed pages.
     *
     * @return {@link PagedEmbed}.
     */
    public PagedEmbed build() {
        boolean pageCounts = false;
        if (list.getPages() > 1) {
            pageCounts = true;
        }
        boolean hasCodeBlock = false;
        if (codeBlock != null) {
            hasCodeBlock = true;
        }
        return new PagedEmbed(title, codeBlock, hasCodeBlock, list, pageCounts);
    }

    public class PagedEmbed {
        private String title;
        private String codeBlock;
        private boolean hasCodeBlock;
        private PaginationList<T> list;
        private boolean pageCounts;
        private int pageTotal;

        public PagedEmbed(String title, String codeBlock, boolean hasCodeBlock, PaginationList<T> list, boolean pageCounts) {
            this.title = title;
            this.pageCounts = pageCounts;
            pageTotal = list.getPages();
            this.codeBlock = codeBlock;
            this.hasCodeBlock = hasCodeBlock;
            this.list = list;
        }

        /**
         * Gets the {@link MessageEmbed} for a specified page.
         *
         * @param page The page to get an embed.
         * @return the {@link MessageEmbed} page.
         */
        public MessageEmbed getEmbed(int page) {
            EmbedBuilder pageEmbed = new EmbedBuilder();
            if (title != null) {
                pageEmbed.setTitle(title);
            }
            pageEmbed.addField("Info", (hasCodeBlock ? "```" + codeBlock + "\n" : "") + list.getPage(page) + (hasCodeBlock ? "```" : ""), false);
            if (pageCounts) {
                pageEmbed.addField("Page", String.valueOf(page + 1), true);
                pageEmbed.addField("Total Pages", String.valueOf(list.getPages()), true);
            }
            return pageEmbed.build();
        }

        /**
         * Gets weather or not this is single paged.
         *
         * @return if it's single paged.
         */
        public boolean isSinglePage() {
            return list.getPages() == 0;
        }

        /**
         * Gets the total amount of paged.
         *
         * @return The total amount of paged.
         */
        public int getPageTotal() {
            return pageTotal;
        }
    }
}
