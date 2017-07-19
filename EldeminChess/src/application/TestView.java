package application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.gson.Gson;

import chess.ai.ChessAI;
import chess.ai.ChessAICurrent;
import chess.ai.SearchNode;
import chess.instance.board.Game;
import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import chess.rule.piece.move.Move;
import chess.rule.piece.move.MoveImpl;
import chess.rule.set.RuleSet;
import chess.rule.set.RuleSetData;
import chess.rule.set.RuleSetImpl;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TestView {
	
	private RuleSet ruleSet;// = new RuleSetImpl();

	private ChessAI[] ais = new ChessAI[]{ new ChessAICurrent(), new ChessAICurrent() };
	private SearchNode bestNode;

	private Game game;
	private BoardView boardView = new BoardView();

	private Button btnReset = new Button("Reset");
	private Button btnUndo = new Button("Undo");
	private Button btnRedo = new Button("Redo");
	private Button btnStartAi = new Button("AI Start");
	private Button btnStopAi = new Button("AI Stop");
	private Button btnMoveAi = new Button("AI Move");
	private Button btnTestJson = new Button("Get Test Json");
	private HBox btnBox = new HBox(btnReset, btnUndo, btnRedo, btnStartAi, btnStopAi, btnMoveAi, btnTestJson);
	
	private Slider aiTimeSlider = new Slider(1, 50, 5);
	private Label aiTimeLabel = new Label();
	private CheckBox aiAutoPlay = new CheckBox("Auto play");
	private CheckBox aiPlay = new CheckBox("Computer Player");
	private ProgressBar aiProgress = new ProgressBar();
	private HBox aiBox = new HBox(aiTimeSlider, aiTimeLabel, aiAutoPlay, aiPlay, aiProgress);
	
	private Label labelAI = new Label("AI sleeping");
	
	private VBox root = new VBox(boardView.getRegion(), btnBox, aiBox, labelAI);
	
	private List<Move> moveHistory = new ArrayList<>();
	private int moveNo = -1;
	
	public TestView() throws IOException {
		String json = new String(Files.readAllBytes(Paths.get("data/testrule/testrule.json")));
		RuleSetData ruleSetData = new Gson().fromJson(json, RuleSetData.class);
		ruleSet = new RuleSetImpl(ruleSetData);
		game = new Game(ruleSet);
		
		
		game.setGameState(ruleSet.getInitialState());
		boardView.showBoard(game);
		
		boardView.setOnMoveTargetted(pos -> {
			doMove(new MoveImpl(pos[0], pos[1], false, -1));
			if (aiPlay.isSelected()) {
				startAI();
			}
			return null;
		});
		
		Runnable onEvaluationFinished = () -> Platform.runLater(() -> {
			aiMove();
			if (aiAutoPlay.isSelected()) {
				startAI();
			}
		});
		
		btnReset.setOnAction(e -> {
			game.setGameState(ruleSet.getInitialState());
			boardView.showBoard(game);
		});

		btnUndo.setOnAction(e -> {
			if (moveNo >= 0) {
				moveHistory.get(moveNo).unmake(game.getGameState(), ruleSet);
				boardView.showBoard(game);
				moveNo--;
			}
		});

		btnRedo.setOnAction(e -> {
			if (moveNo < moveHistory.size() - 1) {
				moveNo++;
				moveHistory.get(moveNo).make(game.getGameState(), ruleSet);
				boardView.showBoard(game);
			}
		});
		
		btnStartAi.setOnAction(e -> {
			startAI();
		});
		
		btnStopAi.setOnAction(e -> {
			ChessAI ai = ais[game.getGameState().getTurn()];
			ai.setOnEvaluationFinished(null);
			ai.stop();
			ai.setOnEvaluationFinished(onEvaluationFinished);
		});
		
		btnMoveAi.setOnAction(e -> {
			aiMove();
		});
		
		btnTestJson.setOnAction(e -> {
			printTestJson();
		});
		
		for (ChessAI ai : ais) {
			ai.setOnEvaluationFinished(onEvaluationFinished);
		}
		
		initAiBox();
	}

	private void initAiBox() {
		aiPlay.setSelected(true);
		aiTimeSlider.valueProperty().addListener(e -> {
			aiTimeLabel.setText(getAiTime() + " ms");
		});
		aiTimeSlider.setValue(15);
		aiTimeSlider.setMinorTickCount(1);
		aiTimeSlider.setMajorTickUnit(100);
		aiProgress.setVisible(false);
	}

	private long getAiTime() {
		if (aiTimeSlider.getValue() == aiTimeSlider.getMax()) {
			return 1_000_000_000;
		} else {
			return (long)aiTimeSlider.getValue() * 200;
		}
	}

	private void printTestJson() {
		String json;
		try {
			json = new String(Files.readAllBytes(Paths.get("data/testrule/testrule.json")));
			RuleSetData ruleSetData = new Gson().fromJson(json, RuleSetData.class);
			GameState state = game.getGameState();
			ruleSetData.turn = state.getTurn();
			state.getSize().forEach(pos -> {
				Piece piece = state.getPiece(pos);
				if (piece != null) {
					ruleSetData.startingPosition[pos.y][pos.x] = piece.getId();
					ruleSetData.startingTeam[pos.y][pos.x] = piece.getTeam();
				} else {
					ruleSetData.startingPosition[pos.y][pos.x] = -1;
					ruleSetData.startingTeam[pos.y][pos.x] = -1;
				}
			});
			System.out.println(new Gson().toJson(ruleSetData));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void startAI() {
		Function<SearchNode, Void> onNewBestNode = node -> {
			bestNode = node;
			Platform.runLater(() -> {
				if (node != null) {
					labelAI.setText(node.toString());
				}
			});
			return null;
		};

		startMs = System.currentTimeMillis();
		aiTime = getAiTime();
		timer.start();
		aiProgress.setVisible(true);
		
		ChessAI ai = ais[game.getGameState().getTurn()];
		ai.start(ruleSet, game.getGameState().copy(), onNewBestNode, aiTime);
		ai.setOnDebug(gameState -> {
			Platform.runLater(() -> {
				game.setGameState(gameState);
				boardView.showBoard(game);				
			});
			return null;
		});
	}

	private void aiMove() {
		if (bestNode != null) {
			ChessAI ai = ais[game.getGameState().getTurn()];
			ai.stop();
			timer.stop();
			aiProgress.setVisible(false);
			doMove(bestNode.getBestChild().getMove());
		}
	}
	
	private void doMove(Move move) {
		moveNo++;
		for (int i = moveNo; i < moveHistory.size(); i++) {
			moveHistory.remove(i);
		}
		moveHistory.add(move);
		move.make(game.getGameState(), ruleSet);
		boardView.showBoard(game);
	}

	public Region getRegion() {
		return root;
	}
	
	private long startMs;
	private long aiTime;
	private AnimationTimer timer = new AnimationTimer() {
		@Override
		public void handle(long now) {
			double progress = (double)(System.currentTimeMillis() - startMs) / aiTime;
			aiProgress.setProgress(progress);
		}
	};
	
}
