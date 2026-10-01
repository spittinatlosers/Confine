package confine.check;

import confine.KeyDefinition;
import confine.bind.BoundMember;
import confine.bind.ClassBinding;
import confine.node.Block;
import confine.node.Node;
import confine.node.Nodes;

import java.util.ArrayList;
import java.util.List;

public final class ValidationEngine {

    public ValidationResult validate(Block root, ClassBinding binding) {
        if (root == null) {
            throw new NullPointerException("root");
        }
        if (binding == null) {
            throw new NullPointerException("binding");
        }
        List<ValidationError> errors = new ArrayList<>();
        for (BoundMember member : binding.members()) {
            if (member.ignored() || (member.validators().isEmpty() && !member.required())) {
                continue;
            }
            Node node = Nodes.find(root, member.path());
            if (node == null) {
                if (member.required()) {
                    errors.add(new ValidationError(member.path(), "value is required"));
                }
                continue;
            }
            Object value = Nodes.plain(node);
            for (Validator<Object> validator : member.validators()) {
                ValidationError error = validator.validate(member.path(), value);
                if (error != null) {
                    errors.add(error);
                }
            }
        }
        return new ValidationResult(errors);
    }

    public ValidationResult validate(Block root, List<KeyDefinition> definitions) {
        if (root == null) {
            throw new NullPointerException("root");
        }
        if (definitions == null) {
            throw new NullPointerException("definitions");
        }
        List<ValidationError> errors = new ArrayList<>();
        for (KeyDefinition definition : definitions) {
            Node node = Nodes.find(root, definition.path());
            if (node == null) {
                if (definition.required()) {
                    errors.add(new ValidationError(definition.path(), "value is required"));
                }
                continue;
            }
            Object value = Nodes.plain(node);
            for (Validator<Object> validator : definition.validators()) {
                ValidationError error = validator.validate(definition.path(), value);
                if (error != null) {
                    errors.add(error);
                }
            }
        }
        return new ValidationResult(errors);
    }
}
