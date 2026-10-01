package confine.check;

import confine.internal.Texts;
import confine.node.Block;
import confine.node.Node;
import confine.node.Note;

public final class DefaultMerger {

    public Block merge(Block user, Block defaults, MergePolicy policy) {
        if (user == null) {
            throw new NullPointerException("user");
        }
        if (defaults == null) {
            throw new NullPointerException("defaults");
        }
        MergePolicy selected = policy == null ? MergePolicy.FILL_MISSING_WITH_COMMENTS : policy;
        if (selected == MergePolicy.PREFER_DEFAULTS) {
            Block result = defaults.copy();
            addMissing(result, user);
            return result;
        }
        Block result = user.copy();
        fillMissing(result, defaults, selected == MergePolicy.FILL_MISSING_WITH_COMMENTS);
        return result;
    }

    public Block overlay(Block base, Block incoming) {
        if (base == null) {
            throw new NullPointerException("base");
        }
        if (incoming == null) {
            throw new NullPointerException("incoming");
        }
        Block result = base.copy();
        applyIncoming(result, incoming);
        return result;
    }

    private void fillMissing(Block target, Block source, boolean copyComments) {
        for (Node child : source.ordered()) {
            if (child instanceof Note || child.name() == null) {
                continue;
            }
            Node existing = target.get(child.name());
            if (existing == null) {
                target.put(child.name(), child.copy());
                continue;
            }
            if (existing instanceof Block && child instanceof Block) {
                Block left = (Block) existing;
                Block right = (Block) child;
                fillMissing(left, right, copyComments);
            }
            if (copyComments) {
                copyComments(child, existing);
            }
        }
    }

    private void addMissing(Block target, Block extra) {
        for (Node child : extra.ordered()) {
            if (child instanceof Note || child.name() == null) {
                continue;
            }
            Node existing = target.get(child.name());
            if (existing == null) {
                target.put(child.name(), child.copy());
                continue;
            }
            if (existing instanceof Block && child instanceof Block) {
                Block left = (Block) existing;
                Block right = (Block) child;
                addMissing(left, right);
            }
        }
    }

    private void applyIncoming(Block target, Block incoming) {
        for (Node child : incoming.ordered()) {
            if (child instanceof Note || child.name() == null) {
                continue;
            }
            Node existing = target.get(child.name());
            if (existing instanceof Block && child instanceof Block) {
                Block left = (Block) existing;
                Block right = (Block) child;
                applyIncoming(left, right);
                copyComments(child, left);
                continue;
            }
            Node replacement = child.copy();
            if (existing != null && replacement.comments().isEmpty()) {
                replacement.comments().addAll(existing.comments());
            }
            if (existing != null
                    && (replacement.inlineComment() == null || Texts.blank(replacement.inlineComment()))) {
                replacement.setInlineComment(existing.inlineComment());
            }
            target.put(child.name(), replacement);
        }
    }

    private void copyComments(Node from, Node to) {
        if (to.comments().isEmpty() && !from.comments().isEmpty()) {
            to.comments().addAll(from.comments());
        }
        if ((to.inlineComment() == null || Texts.blank(to.inlineComment()))
                && from.inlineComment() != null
                && !Texts.blank(from.inlineComment())) {
            to.setInlineComment(from.inlineComment());
        }
    }
}
