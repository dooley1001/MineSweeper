import java.util.Random;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class game extends Application {


    private NumberDisplay mineCount = new NumberDisplay(DIGITS);
    private int numMine;
    private TimeDisplay time = new TimeDisplay(DIGITS); 
    private Board board;
    private MainGame mainGame;



    public static void main(String[] a){
        launch(args);
    }

    public void start(Stage stage) throws Exception {
        board = new Board(9, 9);
        mainGame = new MainGame(board, Difficulty.EASY);

        updateMineCount();

        HBox numberLayout = new HBox(10);
        VBox mainLayout = new VBox(10);

        numberLayout.getChildren().addAll(time, mineCount);
        mainLayout.getChildren().addAll(numberLayout, mainGame);

        Scene scene = new Scene(mainLayout);

        stage.setScene(scene);
        time.start();
        stage.show();

        mainGame.setOnMouseClicked(e -> {

            if (mainGame.isEnd()){

                time.stop();

                if (mainGame.isWin()){
                    win();
                } else {
                    lose();
                }

            } else {

                if (e.getButton().equals(MouseButton.SECONDARY)){
                    updateMineCount();
                }

            }



        });

    }

    private void updateMineCount(){
        numMine = board.getNumMine() - Cell.getNumFlag();
        mineCount.setNumber(numMine);
        mineCount.update();
    }

    private void win(){
        System.out.println("win");
    }

    private void lose(){
        System.out.println("lose" + time.getTime());
    }

    private static final int DIGITS = 3;


}
public class MainGame extends GridPane{

    private ImageView[][] cell;
    private boolean win;
    private boolean end;

    public MainGame(Board board, Difficulty difficulty){

        board.init(difficulty);

        cell = new ImageView[board.getYSize()][board.getXSize()];

        for (int i = 0; i < board.getYSize(); i++){
            for (int j = 0; j < board.getXSize(); j++){

                cell[i][j] = new ImageView(board.getCell(j, i).getUnselectedImage());   
                cell[i][j].setFitHeight(CELL_SIZE);
                cell[i][j].setFitWidth(CELL_SIZE);
                GridPane.setRowIndex(cell[i][j], i + 1);
                GridPane.setColumnIndex(cell[i][j], j + 1);
                this.getChildren().add(cell[i][j]);

            }
        }

        assignEvent(board);

    }

    private void assignEvent(Board board){

        for (ImageView[] cellRow: this.getCell()){
            for (ImageView cell: cellRow){
                cell.setOnMouseClicked(e -> {

                    int[] index = getClickedIndex(cell, board);
                    int x = index[0];
                    int y = index[1];

                    if (e.getButton().equals(MouseButton.SECONDARY)){

                        if (!(board.getCell(x, y).isSelected())){
                            flag(x, y, board);
                        }


                    } else {

                        if (!(board.getCell(x,y).isFlagged())){

                            selectCell(x, y, x, y, board);

                            if (board.getBoardSize() - board.getNumMine() == Cell.getNumSelectedCell()){
                                win();
                            }

                        }                       

                    }

                });
            }
        }

    }

    private void flag(int x, int y, Board board){

        board.getCell(x, y).flag();

        if (board.getCell(x, y).isFlagged()){
            cell[y][x].setImage(board.getCell(x, y).getFlagImage());
        } else {
            cell[y][x].setImage(board.getCell(x, y).getUnselectedImage());
        }

    }

    private void selectCell(int firstX, int firstY, int x, int y, Board board){

        this.cell[y][x].setImage(board.getCell(x, y).getSelectedImage());
        board.getCell(x, y).select();

        if (board.getCell(x,y).getID().equals(CellValue.MINE) && x == firstX && y == firstY){

            lose(board);

        } else if (board.getCell(x,y).getMineCount() == 0){
                selectSurroundingCell(firstX, firstY, x, y, board);         

        }



    }

    private void selectSurroundingCell(int firstX, int firstY, int x, int y, Board board){

        for (int i = (y - 1); i <= (y + 1); i++){
            for (int j = (x - 1); j <= (x + 1); j++){

                try {

                    if (board.getCell(j, i).isSelected()){
                        continue;
                    }

                    if (i == y && j == x){
                        continue;   
                    }

                    selectCell(firstX, firstY, j, i, board);

                } catch (IndexOutOfBoundsException ex){
                    continue;
                }



            }
        }   

    }

    private int[] getClickedIndex(ImageView cell, Board board){

        int[] index = new int[2];

        for (int i = 0; i < board.getYSize(); i++){
            for (int j = 0; j < board.getXSize(); j++){



                if (cell.equals(this.cell[i][j])){
                    index[0] = j;
                    index[1] = i;
                }

            }
        }

        return index;

    }

    private void win(){
        end = true;
        win = true;
    }

    private void lose(Board board){
        displayAll(board);
        end = true;
        win = false;
    }

    public boolean isWin(){
        return win;
    }

    public boolean isEnd(){
        return end;
    }

    private void displayAll(Board board){

        for (int i = 0; i < board.getYSize(); i++){
            for (int j = 0; j < board.getXSize(); j++){

                if (!(board.getCell(j, i).isSelected())){
                    this.cell[i][j].setImage(board.getCell(j, i).getSelectedImage());
                }

            }   
        }
    }


    public ImageView getCell(int x, int y){
        return cell[y][x];
    }

    public ImageView[][] getCell(){
        return cell;
    }

    public static final int CELL_SIZE = 20;

}


public class Board {

    private Cell[][] cells;
    private Random random = new Random();
    private int numMine;

    public Board(int xSize, int ySize){
        cells = new Cell[xSize][ySize];

    }

    public void init(Difficulty difficulty){

        initEmptyCell();
        numMine = initNumMine(difficulty);
        initMine();
        initMineCount();

    }

    public void init(int numMine) throws TooMuchMineException{

        if (numMine >= ((cells.length - 1) * (cells[0].length - 1))){
            throw new TooMuchMineException();
        }

        initEmptyCell();
        this.numMine = numMine;
        initMine();
        initMineCount();
    }

    private void initEmptyCell(){

        for (int i = 0; i < cells.length; i++){
            for (int j = 0; j < cells[0].length; j++){
                cells[i][j] = new Cell();
            }
        }
    }

    private int initNumMine(Difficulty difficulty){

        switch(difficulty){
            case EASY: return getBoardSize() / EASY_FACTOR; 
            case MEDIUM: return getBoardSize() / MEDIUM_FACTOR; 
            case HARD: return getBoardSize() / HARD_FACTOR; 
            default: return 0;
        }
    }

    private void initMine(){

        for (int i = 0; i < numMine; i++){

            while(true){
                Cell randomCell = cells[random.nextInt(cells.length)][random.nextInt(cells[0].length)];

                if (!(randomCell.getID().equals(CellValue.MINE))){
                    randomCell.setMine();
                    break;
                }

            }

        }
    }

    private void initMineCount(){

        for (int i = 0; i < cells.length; i++){
            for (int j = 0; j < cells[0].length; j++){

                if (cells[i][j].getID().equals(CellValue.MINE)){
                    continue;
                }

                int mineCount = 0;

                mineCount = getMineCount(j, i);

                cells[i][j].setMineCount(mineCount);

            }
        }

    }

    public Cell getCell(int x, int y){
        return cells[y][x];
    }

    public Cell[][] getCell(){
        return cells;
    }

    private int getMineCount(int x, int y){

        int mineCount = 0;

        for (int i = (y - 1); i <= (y + 1); i++){
            for (int j = (x - 1); j <= (x + 1); j++){

                if (i == y && j == x) continue;

                try {

                    if (cells[i][j].getID().equals(CellValue.MINE)){
                        mineCount++;
                    }

                } catch (IndexOutOfBoundsException ex){
                    continue;
                }

            }
        }

        return mineCount;


    }

    public int getBoardSize(){
        return getYSize() * this.getXSize();
    }

    public int getXSize(){
        return cells[0].length;
    }

    public int getYSize(){
        return cells.length;
    }

    public int getNumMine(){
        return numMine;
    }


    private static final int EASY_FACTOR = 8;
    private static final int MEDIUM_FACTOR = 6;
    private static final int HARD_FACTOR = 4;



}