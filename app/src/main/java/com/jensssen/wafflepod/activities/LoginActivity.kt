package com.jensssen.wafflepod.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jensssen.wafflepod.R
import com.jensssen.wafflepod.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val TAG: String = "LoginActivity"
    private var user_wants_login: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "Enter Login Activity")

        // Initialize Firebase Auth
        auth = Firebase.auth
        // Check if user is signed in (non-null) to Firebase and update UI accordingly.
        val currentUser = auth.currentUser
        Log.d(TAG, "Enter Login Activity" + currentUser.toString())
        Log.d(TAG, currentUser.toString())

        if(currentUser != null){
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        }

        // Register or login User
        binding.btnLogin.setOnClickListener() {
            if (user_wants_login) {
                loginUser()
            } else {
                createUser()
            }
        }

        // Not registered yet
        binding.tvRegHere.setOnClickListener() {
            if (user_wants_login) {
                user_wants_login = false
                binding.tvlogin.text = getString(R.string.register)
                binding.tvNotReg.text = getString(R.string.already_registered)
                binding.tvRegHere.text = getString(R.string.login_here)
                binding.btnLogin.text = getString(R.string.register)
            } else {
                user_wants_login = true
                binding.btnLogin.text = getString(R.string.login)
                binding.tvlogin.text = getString(R.string.login)
                binding.tvNotReg.text = getString(R.string.not_registered_yet)
                binding.tvRegHere.text = getString(R.string.register_here)
            }
        }
    }

    private fun loginUser() {
        val email: String = binding.etLogInEmail.text.toString()
        val password: String = binding.etLogInPass.text.toString()

        if (TextUtils.isEmpty(email)) {
            binding.etLogInEmail.setError("Email can not be empty!")
            binding.etLogInEmail.requestFocus()
        } else if (TextUtils.isEmpty(password)) {
            binding.etLogInPass.setError("Password con not be empty!")
            binding.etLogInPass.requestFocus()
        } else {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun createUser() {
        val email: String = binding.etLogInEmail.text.toString()
        val password: String = binding.etLogInPass.text.toString()

        if (TextUtils.isEmpty(email)) {
            binding.etLogInEmail.setError("Email can not be empty!")
            binding.etLogInEmail.requestFocus()
        } else if (TextUtils.isEmpty(password)) {
            binding.etLogInPass.setError("Password con not be empty!")
            binding.etLogInPass.requestFocus()
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)

                        Toast.makeText(
                            baseContext,
                            "${task.exception?.message.toString()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}