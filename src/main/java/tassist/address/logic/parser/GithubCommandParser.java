package tassist.address.logic.parser;

import static java.util.Objects.requireNonNull;
import static tassist.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tassist.address.logic.parser.CliSyntax.PREFIX_GITHUB;
import static tassist.address.model.person.StudentId.VALIDATION_REGEX;

import tassist.address.commons.core.index.Index;
import tassist.address.commons.exceptions.IllegalValueException;
import tassist.address.logic.commands.GithubCommand;
import tassist.address.logic.parser.exceptions.ParseException;
import tassist.address.model.person.Github;
import tassist.address.model.person.StudentId;

/**
 * Parses input arguments and creates a new {@code GithubCommand} object
 */
public class GithubCommandParser implements Parser<GithubCommand> {

    /**
     * Parses the given {@code String} of arguments in the context of the {@code GithubCommand}
     * and returns a {@code GithubCommand} object for execution.
     * @throws ParseException if the user input does not conform the expected format
     */
    public GithubCommand parse(String args) throws ParseException {
        requireNonNull(args);
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args,
                PREFIX_GITHUB);
        String trimmedArgs = argMultimap.getPreamble().trim();

        if (trimmedArgs.isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT,
                    GithubCommand.MESSAGE_USAGE));
        }

        argMultimap.verifyNoDuplicatePrefixesFor(PREFIX_GITHUB);

        String github = argMultimap.getValue(PREFIX_GITHUB).orElse("");
        if (argMultimap.getValue(PREFIX_GITHUB).isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT,
                    GithubCommand.MESSAGE_USAGE));
        }
        try {
            Github githubUrl = new Github(github);
        } catch (IllegalArgumentException e) {
            throw new ParseException(GithubCommand.MESSAGE_INVALID_GITHUB, e);
        }

        if (trimmedArgs.matches(VALIDATION_REGEX)) {
            try {
                StudentId studentId = ParserUtil.parseStudentId(trimmedArgs);
                return new GithubCommand(studentId, new Github(github));
            } catch (IllegalValueException ive) {
                throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT,
                        GithubCommand.MESSAGE_USAGE), ive);
            }
        }

        try {
            Index index = ParserUtil.parseIndex(trimmedArgs);
            return new GithubCommand(index, new Github(github));
        } catch (IllegalValueException ive) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT,
                    GithubCommand.MESSAGE_USAGE), ive);
        }
    }
}
