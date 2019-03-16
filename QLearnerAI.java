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
            updateQTable(game, curr_board);
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
        int epsilon = r.nextInt(1); // 0 or 1
        int action;
        if (epsilon == 0) { // set chosenMove to a random, legal column (explore paths)
            action = r.nextInt(legalActions.size());
        }
        else { // set chosenMove to the maximum of q_values for that given state (exploit path)
            action = getMaxQValueAction(q_values);
        }
        return action;
    }

    private void updateQTable(GameStateModule game, Board curr_board){
        // update q(s, a) and count(s, a)
        game.makeMove(chosenMove);
        if (game.isGameOver()) { // then your move was either a +1 or a +0 move
            if (game.getWinner() == 1) {
                curr_board.q_values[chosenMove] = "1"; // +1 to that action made
                state_action_values.put(curr_board.state, curr_board.q_values); // updates q(s,a)
            }
            else if (game.getWinner() == 0) {
                // this is probably unnecessary
                curr_board.q_values[chosenMove] = "0"; // +0 to that action made
                state_action_values.put(curr_board.state, curr_board.q_values); // updates q(s,a)
            }
        }
        else {
            // game is not over after making play as the current player; Thus:
            // simulate opponent move here using the updated game board
            getNextMove(game); // if doing this causes the game to end, assign -1 for chosenMove.
            // Need to somehow backpropagate that -1 result...
        }
    }

    // helper function
    private int getMaxQValueAction(String[] q_values) {
        int[] q_vals = Arrays.stream(q_values).mapToInt(Integer::parseInt).toArray();
        // we want the column associated with the highest Q value,
        // NOT the highest Q value itself.
        int maxColIndex = 0;
        int count = 0;
        for (int num : q_vals) {
            if (num > q_vals[maxColIndex]) {
                maxColIndex = count;
            }
            ++count;
        }

        return maxColIndex;
    }

}
