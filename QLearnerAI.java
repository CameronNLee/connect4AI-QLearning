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
        int epsilon = r.nextInt(2); // 0 or 1
        int action;
        if (epsilon == 0 || !isModified(q_values)) { // set chosenMove to a random, legal column (explore paths)
            action = legalActions.get(r.nextInt(legalActions.size()));
        }
        else { // set chosenMove to the maximum of q_values for that given state (exploit path)
            action = getMaxQValueAction(legalActions, q_values);
        }
        return action;
    }

    private void updateQTable(GameStateModule game, Board curr_board){
        // update q(s, a) and count(s, a)
        /*
        game.makeMove(chosenMove);

        // number of times the chosen action (i.e. chosenMove) for that chosen state
        // was called in the past. We use this to calculate the alpha value.
        int visits = state_action_count.get(curr_board.state)[chosenMove];
        int alphaValue = 1 / (1 + visits);
        double q_value;
        double reward = 0;

        if (game.isGameOver()) { // then your move is rewarded either a +1 or a +0
            reward = (game.getWinner() != 0) ? 1 : 0;
        }
        else {
            // game is not over after making play as the current player; Thus:
            // simulate opponent move here using the updated game board
            getNextMove(game); // if doing this causes the game to end, assign -1 for chosenMove.
            if (game.isGameOver()) {
                if (game.getWinner() != 0) {
                    reward = -1;
                }
                else if (game.getWinner() == 0) {
                    reward = 0;
                }
            }
            game.unMakeMove();
            // Need to somehow backpropagate that -1 result...
        }

        // here is where we update q(s,a)
        // TODO: change q_value into a double type later after changing q_values, from String[] to double[].
        //q_value = ((1 - alphaValue) + alphaValue*(reward + gamma));
        q_value = (int)reward;
        curr_board.q_values[chosenMove] = Double.toString(q_value);
        state_action_values.put(curr_board.state, curr_board.q_values);

        // here is where we update count(s,a) by 1
        state_action_count.get(curr_board.state)[chosenMove] += 1;
        */
    }

    // helper function
    private int getMaxQValueAction(ArrayList<Integer> legalActions, String[] q_values) {
        ArrayList<Integer> q_vals = new ArrayList<Integer>();
        for (String element : q_values) {
            q_vals.add(Integer.valueOf(element));
        }
        // we want the column associated with the highest Q value,
        // NOT the highest Q value itself
        int maxIndex = 0;
        for (int i = 1; i < q_vals.size(); i++) {
            if (legalActions.contains(i) && q_vals.get(i) >= q_vals.get(maxIndex)) {
                maxIndex = i;
            }
        }
        if (!legalActions.contains(maxIndex)) {
            maxIndex = legalActions.get(0);
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
        int[] q_vals = Arrays.stream(q_values).mapToInt(Integer::parseInt).toArray();
        for (int i : q_vals) {
            if (i != 0) {
                return true;
            }
        }
        return false;
    }

}
