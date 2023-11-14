package ru.SpringDemo.SpringBot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.SpringDemo.SpringBot.BotConfig;
import ru.SpringDemo.SpringBot.model.Ads;
import ru.SpringDemo.SpringBot.model.AdsRepository;
import ru.SpringDemo.SpringBot.model.User;
import ru.SpringDemo.SpringBot.model.UserRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    public static final String YES_BUTTON = "YES_BUTTON";
    public static final String NO_BUTTON = "NO_BUTTON";
    public static final String ERROR_OCCURED = "Error occured: ";
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdsRepository adsRepository;
    final BotConfig config;

    static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /help to see this message again";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your stored data"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        listOfCommands.add(new BotCommand("/help", "info, how to use this bot"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            log.error("Error setting bot's command list " + e.getMessage());
        }
    }
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (message.contains("/send") && chatId == config.getOwnerId()) {
                var textToSend = EmojiParser.parseToUnicode(message.substring(message.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user : users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            }
            else {
                switch (message) {
                    case ("/start"):
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case ("/register"):
                        register(chatId);
                        break;
                    case ("/help"):
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    default:
                        prepareAndSendMessage(chatId, "Sorry, command was not recognized");

                }
            }
        }
        else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callBackData.equals(YES_BUTTON)) {
                String text = "You pressed Yes button";
                EditMessageText message = new EditMessageText();

                setMessageProperties(message, text, chatId, messageId);

                executeMessage(message);
            }
            else if (callBackData.equals(NO_BUTTON)) {
                String text = "You pressed No button";
                EditMessageText message = new EditMessageText();

                setMessageProperties(message, text, chatId, messageId);

                executeMessage(message);
            }
        }
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();
        var inlineRow = new ArrayList<InlineKeyboardButton>();

        var yes_button = new InlineKeyboardButton();
        yes_button.setText("YES");
        yes_button.setCallbackData(YES_BUTTON);

        var no_button = new InlineKeyboardButton();
        no_button.setText("NO");
        no_button.setCallbackData(NO_BUTTON);

        inlineRow.add(yes_button);
        inlineRow.add(no_button);
        inlineRows.add(inlineRow);

        inlineKeyboardMarkup.setKeyboard(inlineRows);
        message.setReplyMarkup(inlineKeyboardMarkup);

        executeMessage(message);
    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            var user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:");

        sendMessage(chatId, answer);
        log.info("Replied to user " + name);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);

        var keyBoardMarkUp = new ReplyKeyboardMarkup();
        var keyBoardRows = new ArrayList<KeyboardRow>();
        var keyBoardRow = new KeyboardRow();
        keyBoardRow.add("weather");
        keyBoardRow.add("get random joke");
        keyBoardRows.add(keyBoardRow);

        keyBoardRow = new KeyboardRow();
        keyBoardRow.add("register");
        keyBoardRow.add("check my data");
        keyBoardRow.add("delete my data");
        keyBoardRows.add(keyBoardRow);

        keyBoardMarkUp.setKeyboard(keyBoardRows);
        sendMessage.setReplyMarkup(keyBoardMarkUp);

        executeMessage(sendMessage);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            log.error(ERROR_OCCURED + e.getMessage());
        }
    }

    private void executeMessage(EditMessageText message) {
        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            log.error(ERROR_OCCURED + e.getMessage());
        }
    }

    private void setMessageProperties(EditMessageText message, String text, long chatId, long messageId) {
        message.setChatId(String.valueOf(chatId));
        message.setMessageId((int) messageId);
        message.setText(text);
    }

    private void prepareAndSendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    @Scheduled(cron = "${cron.scheduler}")
    private void sendAds() {
        var ads = adsRepository.findAll();
        var users = userRepository.findAll();

        for (Ads ad : ads) {
            for (User user : users) {
                prepareAndSendMessage(user.getChatId(), ad.getAd());
            }
        }
    }
}
