package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.model.person.Group;
import seedu.address.model.person.Person;

/**
 * Represents the in-memory model of the address book data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final AddressBook addressBook;

    private final AddressBook groupAddressBook;
    private final UserPrefs userPrefs;
    private FilteredList<Person> filteredPersons;
    private CommandHistory commandHistory = new CommandHistory();

    private boolean lastActionWasGroup = false;
    private FilteredList<Person> filteredGroupPerson;

    private CommandHistory groupCommmandHistory = new CommandHistory();

    private Group group = new Group();


    /**
     * Initializes a ModelManager with the given addressBook and userPrefs.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, ReadOnlyUserPrefs userPrefs) {
        requireAllNonNull(addressBook, userPrefs);

        logger.fine("Initializing with address book: " + addressBook + " and user prefs " + userPrefs);

        this.addressBook = new AddressBook(addressBook);
        //creates an empty addressbook for group.
        this.groupAddressBook = new AddressBook();
        this.userPrefs = new UserPrefs(userPrefs);
        filteredPersons = new FilteredList<>(this.addressBook.getPersonList());
        filteredGroupPerson = new FilteredList<>(this.groupAddressBook.getPersonList());
    }

    public ModelManager() {
        this(new AddressBook(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public void setUserPrefs(ReadOnlyUserPrefs userPrefs) {
        requireNonNull(userPrefs);
        this.userPrefs.resetData(userPrefs);
    }
    @Override
    public void groupPerson(String category) {
        ArrayList<Person> persons = new ArrayList<>(addressBook.getPersonList());
        for (Person person: persons) {
            person.toCompare(category);
        }
        Collections.sort(persons);
        group.group(persons, category);
        saveGroupAddressBookState();
        groupAddressBook.setPersons(group.getGroupList());
        filteredGroupPerson = new FilteredList<>(groupAddressBook.getPersonList());
    }


    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    @Override
    public Path getAddressBookFilePath() {
        return userPrefs.getAddressBookFilePath();
    }

    @Override
    public void setAddressBookFilePath(Path addressBookFilePath) {
        requireNonNull(addressBookFilePath);
        userPrefs.setAddressBookFilePath(addressBookFilePath);
    }

    //=========== AddressBook ================================================================================

    @Override
    public void setAddressBook(ReadOnlyAddressBook addressBook) {
        this.addressBook.resetData(addressBook);
        this.groupAddressBook.resetData(addressBook);
    }

    @Override
    public void setGroupAddressBook(ReadOnlyAddressBook addressBook) {
        this.groupAddressBook.resetData(addressBook);
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return addressBook;
    }

    @Override
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return addressBook.hasPerson(person);
    }

    @Override
    public void deletePerson(Person target) {
        addressBook.removePerson(target);
    }

    @Override
    public void addPerson(Person person) {
        addressBook.addPerson(person);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);

        addressBook.setPerson(target, editedPerson);
    }
    @Override
    public void undo() {
        System.out.println("Non-group command history before: " + commandHistory.size());
        System.out.print("Group command history before : " + groupCommmandHistory.size() + System.lineSeparator());
        if (lastActionWasGroup && !groupCommmandHistory.isEmpty() && groupCommmandHistory.size() != 1) {
            undoGrouping();
            lastActionWasGroup = true; // Reset flag after undoing
        } else if (lastActionWasGroup && groupCommmandHistory.size() == 1) {
            undoGrouping();
            lastActionWasGroup = false;
        } else if (!commandHistory.isEmpty()) {
            setAddressBook(commandHistory.pop());
        }
        System.out.println("Non-group command history after: " + commandHistory.size());
        System.out.print("Group command history after: " + groupCommmandHistory.size() + System.lineSeparator());
    }
    private void undoGrouping() {
        setGroupAddressBook(groupCommmandHistory.pop());
    }
    @Override
    public boolean canUndo() {
        return !commandHistory.isEmpty();
    }
    @Override
    public boolean canUndoGrouping() {
        return lastActionWasGroup && !groupCommmandHistory.isEmpty();
    }
    @Override
    public void saveAddressBookState() {
        commandHistory.push(new AddressBook(addressBook));
        lastActionWasGroup = false; // The last action is not a group operation
    }
    @Override
    public void saveGroupAddressBookState() {
        groupCommmandHistory.push(new AddressBook(groupAddressBook));
        lastActionWasGroup = true;
    }
    @Override
    public void savePersonState(Person before, Person after) {
        AddressBook withEditedPerson = new AddressBook(addressBook);
        withEditedPerson.replacePerson(before, after);
        commandHistory.push(withEditedPerson);
        lastActionWasGroup = false;
    }
    @Override
    public void replacePerson(Person target, Person replacing) {
        this.addressBook.replacePerson(target, replacing);
    }
    //=========== Filtered Person List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Person} backed by the internal list of
     * {@code versionedAddressBook}
     */
    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return filteredPersons;
    }

    @Override
    public ObservableList<Person> getFilteredGroupPersonList() {
        return filteredGroupPerson;
    }

    @Override
    public void updateFilteredPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        filteredPersons.setPredicate(predicate);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ModelManager)) {
            return false;
        }

        ModelManager otherModelManager = (ModelManager) other;
        return addressBook.equals(otherModelManager.addressBook)
                && userPrefs.equals(otherModelManager.userPrefs)
                && filteredPersons.equals(otherModelManager.filteredPersons);
    }

}
