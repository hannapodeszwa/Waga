package pl.polsl.waga

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_all_products.*


class AllProducts : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_products)


        val exitButton: Button = findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            createLayoutDynamically(10);
            val myIntent = Intent(this, MainActivity::class.java)
            startActivity(myIntent)
        }
        /////////////////////////////
        createLayoutDynamically(10);
        ///////////////////////////
    }
    private fun createLayoutDynamically(n: Int) {
       // val baseLayout = findViewById<TableLayout>(R.id.table)
        val baseLayout = findViewById<LinearLayout>(R.id.linearLayout)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        for (i in 0..n) {
            var myButton:Button = Button(this);
            myButton.setText("Button :"+i);
            myButton.setId(i);
           // myButton.width = 10 + i
            //myButton.height =20 + i
            myButton.x = 10.0f + i*25.0f
            myButton.y = 10.0f + i*25.0f
            var id_ = myButton.getId();

            //val ll = LinearLayout(R.id.linearLayout)
            baseLayout.addView(myButton);
           // ll.addView(myButton, lp)

            myButton.setOnClickListener {
                Toast.makeText(this,
                    "Button clicked index = " + id_, Toast.LENGTH_SHORT)
                    .show();
            }
        }
    }
}