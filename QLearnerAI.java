import java.lang.reflect.Array;
import java.util.*;
import java.io.*;


public class QLearnerAI extends AIModule{

    private static double gamma = 0.99;
    public static HashMap<String, int[]> state_action_count = new HashMap<>();
    public static HashMap<String, String[]> state_action_values = new HashMap<>();
    int is_training;

    public QLearnerAI(int is_training){
        this.is_training = is_training;
    }

    class Board{
        String state;
        ArrayList<Integer> legalActions;
        String[] q_values;
        public Board(ArrayList<Integer> legalActions, String state, String[] q_values){
            this.legalActions = legalActions;
            this.state = state;
            this.q_values = q_values;
        }

    }


    @Override
    public void getNextMove(GameStateModule game) {
        if (is_training == 1){
            Board curr_board = getStateActionValues(game);
            chosenMove = selectMove(curr_board.legalActions, curr_board.q_values);
            int chosenMoveCopy = chosenMove;
            updateQTable(game, curr_board, chosenMoveCopy);
        }else{
            try{
                Board curr_board = getStateActionValuesFromFile(game);
                chosenMove = selectMove(curr_board.legalActions, curr_board.q_values);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }


    private Board getStateActionValuesFromFile(GameStateModule game) throws Exception{
        String currState = "";
        int nonzeros = 0;
        ArrayList<Integer> legalActions = new ArrayList<>();
        for(int i=0;i<game.getWidth();i++) {
            // legal actions
            if (game.canMakeMove(i)) {
                legalActions.add(i);
            }
            // current state
            for (int j = 0; j < game.getHeight(); j++) {
                currState += String.valueOf(game.getAt(i, j));
                if (game.getAt(i, j) != 0)
                    nonzeros += 1;
            }
        }
        File f = new File("qtables/" + nonzeros + ".txt");
        BufferedReader br = new BufferedReader(new FileReader(f));
        String[] q_values = new String[game.getWidth()];
        for (int i=0;i<q_values.length;i++){
            q_values[i] = "0";
        }
        String line;
        while ((line = br.readLine())!=null){
            line = line.substring(0, line.length()-1);
            String[] spl = line.split(":");
            if (spl[0].equals(currState)) {
                q_values = spl[1].split(" ");
                break;
            }
        }
        return new Board(legalActions, currState, q_values);
    }


    private Board getStateActionValues(GameStateModule game){
        String currState = "";
        ArrayList<Integer> legalActions = new ArrayList<>();
        for(int i=0;i<game.getWidth();i++){

            if (game.canMakeMove(i)){
                legalActions.add(i);
            }

            for(int j=0;j<game.getHeight();j++){
                currState += String.valueOf(game.getAt(i, j));
            }
        }

        String[] q_values = state_action_values.get(currState);
        if (q_values == null){
            String[] action_values = new String[game.getWidth()];
            for (int i = 0; i < game.getWidth(); i++)
                action_values[i] = "0";
            q_values = action_values;
            state_action_values.put(currState, q_values);
            state_action_count.put(currState, new int[game.getWidth()]);
        }
        return new Board(legalActions, currState, q_values);

    }

    private int selectMove(ArrayList<Integer> legalActions, String[] q_values){
        final Random r = new Random();
        int epsilon = r.nextInt(2); // 0 or 1
        int action;
        if (epsilon == 0 || !isModified(q_values)) { // set chosenMove to a random, legal column (explore paths)
            System.out.println(legalActions.size());
            action = legalActions.get(r.nextInt(legalActions.size()));
        }
        else { // set chosenMove to the maximum of q_values for that given state (exploit path)
            action = getMaxQValueAction(legalActions, q_values);
        }
        return action;
    }

    private void updateQTable(GameStateModule game, Board curr_board, int chosenMoveCopy){
        // update q(s, a) and count(s, a)
        game.makeMove(chosenMoveCopy);

        Boolean playerWon = false;
        Double reward = 0.0;
        Double maxQValue = 0.0;
        Integer visits = state_action_count.get(curr_board.state)[chosenMoveCopy];
        Double alphaValue = 1.0 / (1.0 + visits);
        Double q = Double.valueOf(curr_board.q_values[chosenMoveCopy]);

        if (game.isGameOver()) { // player won, or there was a draw on the player's move
            reward = (game.getWinner() != 0) ? 1 : 0.5;
            playerWon = true;
            game.unMakeMove();
        }
        else {
            Board opponentBoard = getStateActionValues(game);
            game.makeMove(selectMove(opponentBoard.legalActions, opponentBoard.q_values));
        }

        if (game.isGameOver() && !playerWon) { // opponent won the game, or there was a draw on the opponent's move
            if (game.getWinner() != 0) {
                reward = -1.0;
            }
            else if (game.getWinner() == 0) {
                reward = 0.0;
            }
        }
        else {
            Board sPrime = getStateActionValues(game);
            maxQValue = Double.valueOf(sPrime.q_values[getMaxQValueAction(sPrime.legalActions, sPrime.q_values)]);
            //q = reward + gamma * maxQValue; //deterministic

            game.unMakeMove();
            game.unMakeMove();
        }

        // here is where we update q(s,a)
        // TODO: change q_value into a double type later after changing q_values, from String[] to double[].
        q = ((1-alphaValue) * q) + (alphaValue * (reward + gamma * maxQValue));

        curr_board.q_values[chosenMoveCopy] = Double.toString(q);
        state_action_values.put(curr_board.state, curr_board.q_values);

        // here is where we update count(s,a) by 1
        state_action_count.get(curr_board.state)[chosenMoveCopy] += 1;
        game.makeMove(chosenMoveCopy);
    }

    // helper function
    private double getMaxQValue(String[] q_values) {
        ArrayList<Double> q_vals = new ArrayList<Double>();
        for (String element : q_values) {
            q_vals.add(Double.valueOf(element));
        }
        return Collections.max(q_vals);
    }

    private int getMaxQValueAction(ArrayList<Integer> legalActions, String[] q_values) {
        ArrayList<Double> q_vals = new ArrayList<Double>();
        for (String element : q_values) {
            q_vals.add(Double.valueOf(element));
        }
        // we want the column associated with the highest Q value,
        // NOT the highest Q value itself
        int maxIndex = 0;
        for (int i = 1; i < q_vals.size(); i++) {
            if (legalActions.contains(i) && q_vals.get(i) >= q_vals.get(maxIndex)) {
                maxIndex = i;
            }
        }
        Random r = new Random();
        if (!legalActions.contains(maxIndex)) {
            maxIndex = legalActions.get(r.nextInt(legalActions.size()));
        }
        /*
        System.out.print("Q Vals: ");
        System.out.println(q_vals);
        System.out.print("Legal Actions: ");
        System.out.println(legalActions);
        System.out.println(maxIndex);
        */
        return maxIndex;
    }

    private boolean isModified(String[] q_values) {
        ArrayList<Double> q_vals = new ArrayList<Double>();
        for (String element : q_values) {
            q_vals.add(Double.valueOf(element));
        }
        for (Double i : q_vals) {
            if (i != 0) {
                return true;
            }
        }
        return false;
    }

}
