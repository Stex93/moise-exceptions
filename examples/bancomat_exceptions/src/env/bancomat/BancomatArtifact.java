package bancomat;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import cartago.tools.GUIArtifact;

public class BancomatArtifact extends GUIArtifact {

	private BancomatFrame frame;

	private String amountString;
	private int amountInt;

	public void setup() {
		frame = new BancomatFrame();

		linkActionEventToOp(frame.okButton, "ok");
		linkKeyStrokeToOp(frame.text, "ENTER", "ok");
		linkWindowClosingEventToOp(frame, "closed");

		frame.setVisible(true);
	}

	@INTERNAL_OPERATION
	void ok(ActionEvent ev) {
		amountString = frame.getText();
		signal("inputReceived");
	}

	@OPERATION
	public void enableInput() {
		frame.enableInput();
	}

	@OPERATION
	public void disableInput() {
		frame.disableInput();
	}

	@OPERATION
	public void getAmountAsString(OpFeedbackParam<String> res) {
		res.set(amountString);
	}

	@OPERATION
	public void parseAmount() {
		if (amountString == null) {
			failed("nan");
		}

		int result = 0;
		boolean negative = false;
		int i = 0, len = amountString.length();
		int limit = -Integer.MAX_VALUE;
		int multmin;
		int digit;

		if (len > 0) {
			char firstChar = amountString.charAt(0);
			if (firstChar < '0') { // Possible leading "+" or "-"
				if (firstChar == '-') {
					negative = true;
					limit = Integer.MIN_VALUE;
				} else if (firstChar != '+')
					failed("nan");

				if (len == 1) // Cannot have lone "+" or "-"
					failed("nan");
				i++;
			}
			multmin = limit / 10;
			while (i < len) {
				// Accumulating negatively avoids surprises near MAX_VALUE
				digit = Character.digit(amountString.charAt(i++), 10);
				if (digit < 0) {
					failed("nan");
				}
				if (result < multmin) {
					failed("nan");
				}
				result *= 10;
				if (result < limit + digit) {
					failed("nan");
				}
				result -= digit;
			}
		} else {
			failed("nan");
		}
		amountInt = negative ? result : -result;
	}
	
	@OPERATION
	public void giveMoney() {
		frame.giveMoney(amountInt);
	}
	
	@OPERATION
	public void finishWithdraw() {
		frame.finish();
		amountString = "";
		amountInt = 0;
	}
	
	@OPERATION
	public void failWithdraw() {
		frame.fail();
	}
	
	@OPERATION
	public void closeBancomat() {
		frame.dispose();
	}

	class BancomatFrame extends JFrame {

		private JLabel label;
		private JButton okButton;
		private JTextField text;
		private JLabel status;

		public BancomatFrame() {
			setTitle("JaCaMo Bancomat ");
			setSize(400, 130);
			JPanel panel = new JPanel();
			setContentPane(panel);
			okButton = new JButton("Send");
			okButton.setSize(80, 50);
			text = new JTextField(20);
			text.setEditable(false);
			label = new JLabel("Amount:");
			status = new JLabel("Status: READY FOR NEXT CLIENT");
			okButton.setEnabled(false);
			panel.add(label);
			panel.add(text);
			panel.add(okButton);
			panel.add(status);
		}

		void enableInput() {
			text.setEditable(true);
			text.requestFocusInWindow();
			text.setText("");
			okButton.setEnabled(true);
			status.setText("Status: INSERT AMOUNT");
		}

		void disableInput() {
			text.setEditable(false);
			okButton.setEnabled(false);
			status.setText("Status: PARSING AMOUNT...");
		}

		String getText() {
			return text.getText();
		}
		
		void giveMoney(int amount) {
			status.setText("Status: PLEASE WITHDRAW " + amount + "� WITHIN 30 SECONDS");
		}
		
		void finish() {
			status.setText("Status: WITHDRAWAL COMPLETED");
		}
		
		void fail() {
			status.setText("Status: TRY LATER");
		}

	}

}