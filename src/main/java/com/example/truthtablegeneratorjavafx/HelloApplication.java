package com.example.truthtablegeneratorjavafx;


import javafx.application.Application;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;

import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;



import com.example.truthtablegeneratorjavafx.model.*;
import com.example.truthtablegeneratorjavafx.view.*;
import com.example.truthtablegeneratorjavafx.controller.*;


import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        ///******** Help Window **********///
        StackPane stackPane = new StackPane();
        Scene helpScene = new Scene(stackPane);

        Stage helpWindow = new Stage();
        helpWindow.setScene(helpScene);
        helpWindow.setTitle("How to type");

        String imageName = "/question_mark.png";
        helpWindow.getIcons().add(new Image(HelloApplication.class.getResourceAsStream(imageName)));

        Label helpLabel = new Label();
        helpLabel.setStyle("-fx-wrap-text: True;" +
                "-fx-font-size: 18;"
        );

        String helpString = "Enter /\\ and \\/ for logic and and logic or.\n\n" +
                "! or ~ in front of an expression negates it. Use <-> or <=> for iff, -> or => for implies, " +
                "and ^ or xor for exclusive or.  You can also use parentheses to specify order.\n\n" +
                "Variables can be any letter from A-Z, excluding t, f, x, o, and r, which are reserved. " +
                "(Upper and lower case letters will be viewed as different variables).";
        helpLabel.setPadding(new Insets(10, 10, 10, 10));

        helpLabel.setText(helpString);
        stackPane.getChildren().add(helpLabel);


        Model model = new ModelImpl(this);
        Controller controller = new ControllerImpl(model);

        BigMainView bigPapa = new BigMainView(model, controller, stage, helpWindow);

        model.addObserver(bigPapa);

        stage.setScene(bigPapa.getScene());
        stage.setTitle("Truth Table Generator");
        imageName = "/truth_table_gen_javafx_icon.png";
        stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream(imageName)));
        stage.show();






        stage.centerOnScreen();

    }

    public static void main(String[] args) {
        launch();
    }
}